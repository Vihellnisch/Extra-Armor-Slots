package com.extraarmor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which armor column a piece of code sees, so other mods treat the extra column as worn
 * (set bonuses, "wear the full set and press G", armor checks of any style).
 * <p>
 * Minecraft, NeoForge and this mod keep seeing only the normal armor: the extra armor already gets its
 * stats, enchantments and rendering here, and showing it to vanilla too would count it twice.
 * Any other mod reading (or writing) an armor slot, however it gets there ({@code getItemBySlot},
 * {@code getArmorSlots}, {@code getInventory().armor}, {@code getArmor(i)}, {@code getItem(36..39)}),
 * is pointed at:
 * <ul>
 *   <li>the normal piece if it comes from the asking mod,</li>
 *   <li>else the first extra piece that comes from the asking mod (so a mod looking for its own set
 *       finds it, whichever column each piece is in),</li>
 *   <li>else the first extra piece if the normal slot is empty,</li>
 *   <li>else the normal piece, as without this mod.</li>
 * </ul>
 */
public final class ExtraArmorView {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    /** Code that keeps the vanilla view. */
    private static final String[] OWN_CODE = {"net.minecraft.", "com.mojang.", "net.neoforged.", "com.extraarmor."};
    /** Library frames between the asking code and the armor list (iterators, streams, collection helpers). */
    private static final String[] PLUMBING = {"java.", "javax.", "jdk.", "sun.", "kotlin.", "scala.", "com.google.common.",
            "it.unimi.", "org.apache.commons."};
    /** Vanilla methods that only hand the armor list or a slot of it on to whoever called them. */
    private static final Set<String> PASS_THROUGH = Set.of("getItemBySlot", "setItemSlot", "getArmor", "getItem", "setItem",
            "hasItemInSlot", "getArmorSlots", "getArmorAndBodyArmorSlots", "getAllSlots", "get", "set");
    /** item namespace -> module name of the mod jar that registered it ("" if unknown). */
    private static final Map<String, String> MODULES = new ConcurrentHashMap<>();

    /** Who is asking: null for Minecraft/NeoForge/this mod, otherwise the other mod's module (may be unnamed). */
    private record Asker(Module module) {
    }

    /**
     * Which column the code reading/writing this armor slot right now should get: -1 for the normal
     * armor, otherwise the extra set. Called only when at least one extra set has a piece there.
     */
    static int chooseSet(Player player, EquipmentSlot slot, ItemStack normal) {
        Asker asker = asker();
        if (asker == null) return -1;
        if (ownedBy(normal, asker.module())) return -1;
        int firstWorn = -1;
        for (int set = 0; set < ExtraArmor.sets(player); set++) {
            ItemStack extra = ExtraArmor.get(player, slot, set);
            if (extra.isEmpty()) continue;
            if (ownedBy(extra, asker.module())) return set;
            if (firstWorn < 0) firstWorn = set;
        }
        return normal.isEmpty() ? firstWorn : -1;
    }

    private static Asker asker() {
        Optional<StackWalker.StackFrame> frame = WALKER.walk(frames -> frames.dropWhile(ExtraArmorView::isPlumbing).findFirst());
        if (frame.isEmpty()) return null;
        Class<?> cls = frame.get().getDeclaringClass();
        String name = cls.getName();
        for (String prefix : OWN_CODE) {
            if (name.startsWith(prefix)) {
                // other mods' mixin code is merged into vanilla classes under generated names (handler$..., wrapOperation$...)
                return isMergedMixinMethod(frame.get().getMethodName()) ? new Asker(null) : null;
            }
        }
        return new Asker(cls.getModule());
    }

    private static boolean isPlumbing(StackWalker.StackFrame frame) {
        Class<?> cls = frame.getDeclaringClass();
        String method = frame.getMethodName();
        if (cls == ExtraArmorView.class || cls == ExtraArmorList.class || method.contains("extraarmor$")) return true;
        String name = cls.getName();
        for (String prefix : PLUMBING) {
            if (name.startsWith(prefix)) return true;
        }
        boolean armorHolder = LivingEntity.class.isAssignableFrom(cls) || Inventory.class.isAssignableFrom(cls);
        return armorHolder && PASS_THROUGH.contains(method);
    }

    private static boolean isMergedMixinMethod(String method) {
        int dollar = method.indexOf('$');
        return dollar > 0 && !method.startsWith("lambda$") && !method.startsWith("access$") && !method.contains("extraarmor$");
    }

    /** Whether the item comes from the same mod (jar) as the asking code. */
    private static boolean ownedBy(ItemStack stack, Module module) {
        if (stack.isEmpty() || module == null || !module.isNamed()) return false;
        if (stack.getItem().getClass().getModule() == module) return true;
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return module.getName().equals(MODULES.computeIfAbsent(namespace, ExtraArmorView::moduleOf));
    }

    private static String moduleOf(String modId) {
        IModFileInfo file = ModList.get().getModFileById(modId);
        if (file == null) return "";
        try {
            return file.getFile().getSecureJar().name();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private ExtraArmorView() {}
}
