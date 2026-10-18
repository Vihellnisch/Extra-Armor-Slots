package testmod;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stands in for "some other mod" in the tour (its package is not Minecraft, NeoForge or Extra Armor).
 * Each method reads the worn helmet a different way that real mods use.
 */
public final class ArmorCheckStyles {

    /** style name -> the helmet item that style sees. */
    public static Map<String, Item> helmetSeenByEachStyle(Player player) {
        List<ItemStack> iterated = new ArrayList<>();
        player.getArmorSlots().forEach(iterated::add);
        return Map.of(
                "getItemBySlot", player.getItemBySlot(EquipmentSlot.HEAD).getItem(),
                "inventory.armor.get", player.getInventory().armor.get(3).getItem(),
                "inventory.getArmor", player.getInventory().getArmor(3).getItem(),
                "inventory.getItem(39)", player.getInventory().getItem(39).getItem(),
                "getArmorSlots loop", iterated.get(3).getItem(),
                "armor.stream", player.getInventory().armor.stream().skip(3).findFirst().orElse(ItemStack.EMPTY).getItem());
    }

    /** An "unequip helmet" feature of another mod: take the worn helmet off and into the inventory. */
    public static void unequipHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.getInventory().add(helmet);
    }

    /** A sloppy mod that moves the worn helmet object itself into the inventory without clearing the slot. */
    public static void sloppyMoveHelmet(Player player) {
        player.getInventory().add(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    private ArmorCheckStyles() {}
}
