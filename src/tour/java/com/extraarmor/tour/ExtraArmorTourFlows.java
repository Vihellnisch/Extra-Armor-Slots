package com.extraarmor.tour;

import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Client half of the "real input" stops: drives the game the way a player does (clicking slots in the
 * creative / survival inventory, pressing the stratagem keys, jumping to open an elytra) so the server
 * sees exactly the packets a real player sends.
 */
final class ExtraArmorTourFlows {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Progress inside the current stop: the client can skip server tick numbers, so act on "at or after". */
    private static String stop = "";
    private static int step;

    static void tick(Minecraft mc, String name, int within) {
        if (!name.equals(stop)) {
            stop = name;
            step = 0;
        }
        try {
            switch (name) {
                case "creative_place" -> creativePlace(mc, within);
                case "survival_place" -> survivalPlace(mc, within);
                case "stratagem_keys" -> stratagemKeys(mc, within);
                case "elytra_glide", "elytra_glide_back" -> elytraGlide(mc, within);
                case "ten_sets_screen" -> {
                    if (!ExtraArmorTour.SHOWCASE) nextPage(mc, within);
                }
                default -> {
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("TOUR flow {} failed at {}", name, within, e);
        }
    }

    private static void creativePlace(Minecraft mc, int within) throws ReflectiveOperationException {
        if (within >= 20 && step == 0) {
            step = 1;
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player,
                    mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get());
            mc.setScreen(screen);
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY));
        }
        if (within >= 30 && step >= 1 && step <= 4 && mc.screen instanceof CreativeModeInventoryScreen screen) {
            int i = step - 1;
            step++;
            Slot target = null;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.getContainerSlot() == 46 + i && !(slot.container instanceof net.minecraft.world.entity.player.Inventory)) target = slot;
            }
            // pick the piece up from "a creative tab" (creative carried items are client-side), then click the extra slot
            screen.getMenu().setCarried(ExtraArmorTour.helldivers(com.extraarmor.ExtraArmor.SLOTS[i]));
            Method click = CreativeModeInventoryScreen.class.getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ClickType.class);
            click.setAccessible(true);
            click.invoke(screen, target, target == null ? -1 : target.index, 0, ClickType.PICKUP);
            Slot real = mc.player.inventoryMenu.slots.get(46 + i);
            LOGGER.info("TOUR flow creative click extra slot {} (found {} {} idx {}) carried now {} | real slot {} mayPlace={} item={} invOpen={} creative={}",
                    i, target != null, target == null ? "-" : target.getClass().getSimpleName(), target == null ? -1 : target.index,
                    screen.getMenu().getCarried(), real.getClass().getSimpleName(),
                    real.mayPlace(ExtraArmorTour.helldivers(com.extraarmor.ExtraArmor.SLOTS[i])), real.getItem(), screen.isInventoryOpen(), mc.player.isCreative());
        }
        if (within >= 50 && step == 5) {
            step = 6;
            mc.setScreen(null);
        }
    }

    private static void survivalPlace(Minecraft mc, int within) {
        if (within >= 20 && step == 0) {
            step = 1;
            mc.setScreen(new InventoryScreen(mc.player));
        }
        if (within >= 30 && step >= 1 && step <= 4 && mc.screen instanceof InventoryScreen screen) {
            int i = step - 1;
            step++;
            int id = screen.getMenu().containerId;
            mc.gameMode.handleInventoryMouseClick(id, 9 + i, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(id, 46 + i, 0, ClickType.PICKUP, mc.player);
        }
        if (within >= 50 && step == 5) {
            step = 6;
            mc.setScreen(null);
        }
    }

    private static void stratagemKeys(Minecraft mc, int within) throws ReflectiveOperationException {
        if (within < 40 || step != 0) return;
        step = 1;
        Class<?> keys = Class.forName("com.orbitalstrike.client.KeyBindings");
        Class<?> types = Class.forName("com.orbitalstrike.stratagem.StratagemType");
        Object first = types.getEnumConstants()[0];
        int[] code = (int[]) types.getMethod("getCode").invoke(first);
        String[] dirs = {"STRAT_UP", "STRAT_DOWN", "STRAT_LEFT", "STRAT_RIGHT"};
        press(keys, "STRATAGEM_MODE");
        for (int d : code) press(keys, dirs[d]);
        press(keys, "STRATAGEM_MODE");
        LOGGER.info("TOUR flow pressed G, {} arrows, G for {}", code.length, first);
    }

    private static void press(Class<?> keys, String field) throws ReflectiveOperationException {
        KeyMapping mapping = (KeyMapping) keys.getField(field).get(null);
        NeoForge.EVENT_BUS.post(new InputEvent.Key(mapping.getKey().getValue(), 0, GLFW.GLFW_PRESS, 0));
    }

    /** Clicks the ">" page button under the extra columns like a player would. */
    private static void nextPage(Minecraft mc, int within) {
        if (within < 60 || step != 0 || !(mc.screen instanceof InventoryScreen screen)) return;
        step = 1;
        int before = com.extraarmor.client.ExtraArmorClient.layout().page();
        screen.mouseClicked(screen.getGuiLeft() - 7, screen.getGuiTop() + 86 + 8, 0);
        var after = com.extraarmor.client.ExtraArmorClient.layout();
        boolean ok = before == 0 && after.page() == 1 && after.first() == after.perPage();
        LOGGER.info("TOUR CHECK {} client: clicking > shows page 2 (sets {}..{}) (got page {} -> {})", ok ? "PASS" : "FAIL",
                after.first() + 1, after.first() + after.count(), before + 1, after.page() + 1);
    }

    private static void elytraGlide(Minecraft mc, int within) {
        if (within >= 25 && step == 0) {
            step = 1;
            mc.options.keyJump.setDown(true);
        } else if (step == 1) {
            step = 2;
            mc.options.keyJump.setDown(false);
        }
        if (within == 30 || within == 45 || within == 60) {
            LOGGER.info("TOUR flow elytra +{}: client fall flying={} y={} chest(normal)={}", within, mc.player.isFallFlying(),
                    String.format("%.1f", mc.player.getY()), mc.player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    private ExtraArmorTourFlows() {}
}
