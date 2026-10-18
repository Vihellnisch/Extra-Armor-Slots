package com.extraarmor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Replaces {@code Inventory.armor} (feet, legs, chest, head). Stores the normal armor exactly like
 * vanilla, but when another mod reads or writes a slot, {@link ExtraArmorView} may point it at one of
 * the extra sets instead. Every armor accessor (getItemBySlot, getArmorSlots, getArmor, getItem,
 * setItemSlot, direct field use) ends up here, so no mod can miss the extra column.
 */
public final class ExtraArmorList extends NonNullList<ItemStack> {
    private static final EquipmentSlot[] BY_INDEX = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private final Inventory inventory;

    public ExtraArmorList(Inventory inventory) {
        super(Arrays.asList(emptyStacks()), ItemStack.EMPTY);
        this.inventory = inventory;
    }

    private static ItemStack[] emptyStacks() {
        ItemStack[] stacks = new ItemStack[BY_INDEX.length];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    /** The normal armor in that slot, never redirected. */
    public ItemStack normal(int index) {
        return super.get(index);
    }

    @Override
    public ItemStack get(int index) {
        ItemStack normal = super.get(index);
        int set = chosenSet(index, normal);
        return set < 0 ? normal : ExtraArmor.get(inventory.player, BY_INDEX[index], set);
    }

    @Override
    public ItemStack set(int index, ItemStack stack) {
        int set = chosenSet(index, super.get(index));
        if (set < 0) return super.set(index, stack);
        ExtraArmorInventory extra = ExtraArmor.inventory(inventory.player);
        int slot = ExtraArmor.index(set, BY_INDEX[index]);
        ItemStack old = extra.getStackInSlot(slot);
        extra.setStackInSlot(slot, stack);
        return old;
    }

    /** -1 for the normal armor, else which extra set the current caller sees in this slot. */
    private int chosenSet(int index, ItemStack normal) {
        Player player = inventory.player;
        if (player == null || index < 0 || index >= BY_INDEX.length) return -1;
        if (ExtraArmor.first(player, BY_INDEX[index]).isEmpty()) return -1;
        return ExtraArmorView.chooseSet(player, BY_INDEX[index], normal);
    }
}
