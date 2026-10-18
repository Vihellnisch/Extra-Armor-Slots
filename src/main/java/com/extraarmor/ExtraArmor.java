package com.extraarmor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Lookups into the extra armor sets. Set (column) c, piece i lives at storage index c * 4 + i, where
 * piece order is {@link #SLOTS}; set 0 is the column right next to the vanilla armor.
 */
public final class ExtraArmor {
    /** Piece order inside a set: the same top-to-bottom order as the vanilla armor column. */
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Most sets the config allows. The menu always has slots for all of them so client and server always agree. */
    public static final int MAX_SETS = 10;
    public static final int COUNT = MAX_SETS * SLOTS.length;

    /** Sets the server allows, as last told to this client (see ExtraArmorSyncPayload). */
    public static volatile int clientSets = 1;

    public static ExtraArmorInventory inventory(Player player) {
        return player.getData(ExtraArmorMod.EXTRA_ARMOR);
    }

    /** How many extra sets this player can use right now. */
    public static int sets(Player player) {
        return player.level().isClientSide() ? clientSets : ExtraArmorConfig.extraSets();
    }

    public static int index(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
    }

    public static int index(int set, EquipmentSlot slot) {
        return set * SLOTS.length + index(slot);
    }

    /** The piece of extra set {@code set} in that slot (empty for non-players, hands and switched-off sets). */
    public static ItemStack get(LivingEntity entity, EquipmentSlot slot, int set) {
        int i = index(slot);
        if (i < 0 || !(entity instanceof Player player) || set >= sets(player)) return ItemStack.EMPTY;
        return inventory(player).getStackInSlot(set * SLOTS.length + i);
    }

    /** The first extra piece worn in that slot, in set order. */
    public static ItemStack first(LivingEntity entity, EquipmentSlot slot) {
        return prefer(entity, slot, ItemStack.EMPTY, s -> !s.isEmpty());
    }

    /** What renders in a slot: the normal armor if there is any, otherwise the first extra piece. */
    public static ItemStack visible(LivingEntity entity, EquipmentSlot slot, ItemStack worn) {
        return worn.isEmpty() ? first(entity, slot) : worn;
    }

    /**
     * For vanilla "is the player wearing X" checks (turtle helmet, elytra, mob heads...): the worn piece
     * if it qualifies, else the first extra piece that does, else the worn piece.
     */
    public static ItemStack prefer(LivingEntity entity, EquipmentSlot slot, ItemStack worn, Predicate<ItemStack> wanted) {
        if (wanted.test(worn) || !(entity instanceof Player player) || index(slot) < 0) return worn;
        for (int set = 0; set < sets(player); set++) {
            ItemStack extra = get(player, slot, set);
            if (!extra.isEmpty() && wanted.test(extra)) return extra;
        }
        return worn;
    }

    /** Whether any extra armor piece matches. */
    public static boolean anyExtra(LivingEntity entity, Predicate<ItemStack> wanted) {
        if (!(entity instanceof Player player)) return false;
        ExtraArmorInventory inv = inventory(player);
        for (int i = 0; i < sets(player) * SLOTS.length; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && wanted.test(stack)) return true;
        }
        return false;
    }

    /**
     * Vanilla names armor modifiers per slot ("minecraft:armor.helmet", "...respiration/head"), so extra
     * pieces would clash with the normal ones and with each other. Extra armor modifiers get their own ids.
     */
    static ResourceLocation modifierId(int set, EquipmentSlot slot, ResourceLocation original) {
        String prefix = set == 0 ? "extra_" : "extra" + (set + 1) + "_";
        return ResourceLocation.fromNamespaceAndPath(ExtraArmorMod.MODID,
                prefix + slot.getName() + "/" + original.getNamespace() + "/" + original.getPath());
    }

    private ExtraArmor() {}
}
