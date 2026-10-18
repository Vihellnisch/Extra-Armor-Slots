package com.extraarmor.client;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorMod;
import com.extraarmor.ExtraArmorSlot;
import com.extraarmor.mixin.client.SlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the extra armor columns: strips of inventory background growing out of the inventory's left
 * edge, set 1 right next to the vanilla armor column, set 2 left of it, and so on (pieces built from
 * the vanilla texture by generate_panel.py). When the screen is too narrow for every set, the columns
 * are shown a page at a time with arrow buttons under the panel.
 */
@EventBusSubscriber(modid = ExtraArmorMod.MODID, value = Dist.CLIENT)
public final class ExtraArmorClient {
    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath(ExtraArmorMod.MODID, "textures/gui/extra_armor_panel.png");
    /** Texture pieces: left edge (7 px), a middle column (18 px), the column that joins the inventory (18 px). */
    private static final int EDGE = 7, COLUMN = 18, U_MIDDLE = 7, U_JOIN = 25, HEIGHT = 86;

    /** Which sets are on screen: sets [first, first + count), page {@code page} of {@code pages}. */
    public record Layout(int perPage, int page, int pages, int first, int count) {
        boolean shows(int set) {
            return set >= first && set < first + count;
        }

        int width() {
            return EDGE + COLUMN * count;
        }
    }

    private static int page;
    private static Layout layout = new Layout(1, 0, 1, 0, 1);
    private static final List<Button> buttons = new ArrayList<>();

    static {
        ExtraArmorSlot.shownOnClient = set -> layout.shows(set);
    }

    public static Layout layout() {
        return layout;
    }

    /** Whether this screen shows the extra columns right now (not while the recipe book covers them). */
    public static boolean showsPanel(Screen screen) {
        if (screen instanceof InventoryScreen inventory) return !inventory.getRecipeBookComponent().isVisible();
        if (screen instanceof CreativeModeInventoryScreen creative) return creative.isInventoryOpen();
        return false;
    }

    /** Whether the mouse is over the part of the panel outside the inventory's own rectangle. */
    public static boolean overPanel(double mouseX, double mouseY, int left, int top) {
        return mouseX >= left - COLUMN * layout.count() && mouseX < left && mouseY >= top && mouseY < top + HEIGHT;
    }

    private static Layout computeLayout(int guiLeft) {
        int sets = ExtraArmor.clientSets;
        int perPage = Math.max(1, Math.min(sets, (guiLeft - EDGE - 2) / COLUMN));
        int pages = (sets + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        int first = page * perPage;
        return new Layout(perPage, page, pages, first, Math.min(perPage, sets - first));
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        buttons.clear();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        layout = computeLayout(screen.getGuiLeft());
        int y = screen.getGuiTop() + HEIGHT + 2;
        Button back = Button.builder(Component.literal("<"), b -> turn(screen, -1)).bounds(screen.getGuiLeft() - 26, y, 12, 12).build();
        Button next = Button.builder(Component.literal(">"), b -> turn(screen, 1)).bounds(screen.getGuiLeft() - 13, y, 12, 12).build();
        buttons.add(back);
        buttons.add(next);
        event.addListener(back);
        event.addListener(next);
    }

    private static void turn(AbstractContainerScreen<?> screen, int by) {
        Layout now = computeLayout(screen.getGuiLeft());
        page = (page + by + now.pages()) % now.pages();
        layout = computeLayout(screen.getGuiLeft());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        ExtraArmorSlot.hiddenOnClient = screen instanceof InventoryScreen && !showsPanel(screen);
        if (!(screen instanceof AbstractContainerScreen<?> container)) return;
        layout = computeLayout(container.getGuiLeft());
        boolean paging = showsPanel(screen) && layout.pages() > 1;
        for (Button button : buttons) button.visible = paging;
        placeSlots(container);
    }

    /** Moves each extra slot of the shown page into its column (client-side positions only). */
    private static void placeSlots(AbstractContainerScreen<?> screen) {
        boolean creative = screen instanceof CreativeModeInventoryScreen;
        for (Slot slot : screen.getMenu().slots) {
            int index;
            if (slot instanceof ExtraArmorSlot extra) index = extra.getContainerSlot();
            else if (creative && slot.getContainerSlot() >= 46 && !(slot.container instanceof net.minecraft.world.entity.player.Inventory)
                    && slot.getContainerSlot() < 46 + ExtraArmor.COUNT) index = slot.getContainerSlot() - 46;
            else continue;
            int set = index / ExtraArmor.SLOTS.length, piece = index % ExtraArmor.SLOTS.length;
            int column = layout.shows(set) ? set - layout.first() : set;
            ((SlotAccessor) slot).extraarmor$setX(ExtraArmorSlot.X - COLUMN * column);
            ((SlotAccessor) slot).extraarmor$setY(8 + piece * 18);
        }
    }

    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!showsPanel(screen)) return;
        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft(), top = screen.getGuiTop();
        int x = left - COLUMN * layout.count();
        g.blit(PANEL, x, top, 0, 0, EDGE, HEIGHT, 64, 128);
        for (int column = layout.count() - 1; column >= 0; column--) {
            int u = column == 0 ? U_JOIN : U_MIDDLE;
            g.blit(PANEL, left - COLUMN * (column + 1) + EDGE, top, u, 0, COLUMN, HEIGHT, 64, 128);
        }
        if (layout.pages() > 1) {
            String label = (layout.page() + 1) + "/" + layout.pages();
            var font = Minecraft.getInstance().font;
            g.drawString(font, label, left - 28 - font.width(label), top + HEIGHT + 4, 0xFFFFFF, true);
        }
    }

    private ExtraArmorClient() {}
}
