package com.extraarmor.tour;

import com.extraarmor.ExtraArmorMod;
import com.extraarmor.ExtraArmorSlot;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Method;

/**
 * Client half of {@link ExtraArmorTour}: opens (or creates) the test world, opens the screens the
 * "_screen" stops need, screenshots each stop (GUI hidden like F1 outside screens) and quits.
 */
@EventBusSubscriber(modid = ExtraArmorMod.MODID, value = Dist.CLIENT)
public final class ExtraArmorTourClient {
    private static final String WORLD = "extra_armor_test";
    private static boolean opened;
    private static int shot = -1, screenFor = -1;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!ExtraArmorTour.ENABLED || opened || !(event.getScreen() instanceof TitleScreen)) return;
        opened = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0D);
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        if (ExtraArmorTour.SHOWCASE && !mc.getWindow().isFullscreen()) mc.getWindow().toggleFullScreen();
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
        } else {
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261018L, true, false),
                    WorldPresets::createNormalWorldDimensions, event.getScreen());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!ExtraArmorTour.ENABLED || mc.player == null || mc.level == null) return;
        int since = ExtraArmorTour.ticks - ExtraArmorTour.FIRST_STOP;
        if (since < 0) return;
        int stop = since / ExtraArmorTour.STOP_LENGTH, within = since % ExtraArmorTour.STOP_LENGTH;
        if (stop >= ExtraArmorTour.STOP_NAMES.size()) {
            mc.stop();
            return;
        }
        String name = ExtraArmorTour.STOP_NAMES.get(stop);
        boolean screenStop = name.endsWith("_screen");
        mc.options.hideGui = ExtraArmorTour.SHOWCASE || !screenStop; // screens still draw with F1 on
        if (ExtraArmorTour.SHOWCASE) mc.getToasts().clear();
        mc.options.setCameraType(name.endsWith("_front") ? CameraType.THIRD_PERSON_FRONT
                : name.endsWith("_back") ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        if (screenStop && within >= 30 && screenFor != stop) {
            screenFor = stop;
            openScreen(mc, name);
        }
        ExtraArmorTourFlows.tick(mc, name, within);
        boolean flowScreen = name.equals("creative_place") || name.equals("survival_place");
        if (!screenStop && !flowScreen && mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen)) mc.setScreen(null);
        if (name.equals("mod_set_bonus") && within == 60) {
            try {
                Class<?> input = Class.forName("com.orbitalstrike.client.StratagemInputHandler", true, ExtraArmorTourClient.class.getClassLoader());
                boolean ok = (boolean) input.getMethod("hasHelldiverArmor").invoke(null);
                LogUtils.getLogger().info("TOUR CHECK {} client: Orbital Strike stratagem input unlocked by extra-column set (got {})", ok ? "PASS" : "FAIL", ok);
            } catch (ReflectiveOperationException | LinkageError e) {
                LogUtils.getLogger().error("TOUR could not call Orbital Strike client", e);
            }
        }
        if (ExtraArmorTour.SHOWCASE && mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> gui) {
            // park the mouse on the player preview: no slot tooltip, and the player looks at the camera
            int x = gui.getGuiLeft() + (gui instanceof CreativeModeInventoryScreen ? 89 : 51);
            int y = gui.getGuiTop() + (gui instanceof CreativeModeInventoryScreen ? 26 : 40);
            try {
                java.lang.reflect.Field fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
                java.lang.reflect.Field fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
                fx.setAccessible(true);
                fy.setAccessible(true);
                fx.setDouble(mc.mouseHandler, x * mc.getWindow().getGuiScale());
                fy.setDouble(mc.mouseHandler, y * mc.getWindow().getGuiScale());
            } catch (ReflectiveOperationException e) {
                LogUtils.getLogger().error("TOUR could not move the mouse", e);
            }
        }
        if (name.endsWith("_back") || name.endsWith("_front")) {
            // the local player's body turns on its own; line it up with the head for straight shots
            mc.player.setYBodyRot(mc.player.getYRot());
            mc.player.yBodyRotO = mc.player.getYRot();
            mc.player.setYHeadRot(mc.player.getYRot());
            mc.player.yHeadRotO = mc.player.getYRot();
        }
        if (shot != stop && within >= ExtraArmorTour.SHOT_AT) {
            shot = stop;
            if (screenStop) {
                LogUtils.getLogger().info("TOUR screen {}: {} extra slots hidden={}", name,
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(), ExtraArmorSlot.hiddenOnClient);
            }
            Screenshot.grab(mc.gameDirectory, "tour_" + String.format("%02d", stop) + "_" + name + ".png",
                    mc.getMainRenderTarget(), message -> {});
        }
    }

    private static void openScreen(Minecraft mc, String name) {
        mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, name.startsWith("recipe_book"));
        if (name.startsWith("creative")) {
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player,
                    mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get());
            mc.setScreen(screen);
            try {
                Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY));
            } catch (ReflectiveOperationException e) {
                LogUtils.getLogger().error("TOUR could not select the creative inventory tab", e);
            }
        } else {
            mc.setScreen(new InventoryScreen(mc.player));
        }
    }

    private ExtraArmorTourClient() {}
}
