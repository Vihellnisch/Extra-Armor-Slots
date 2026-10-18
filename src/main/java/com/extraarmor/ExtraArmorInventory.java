package com.extraarmor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

/**
 * All extra armor sets of a player: room for {@link ExtraArmor#MAX_SETS} sets, set c piece i at c * 4 + i
 * (piece order head, chest, legs, feet). Sets the config switches off stay empty.
 */
public final class ExtraArmorInventory extends ItemStackHandler {
    /** What the server last applied as attribute modifiers, per slot (never saved). */
    final ItemStack[] applied = empty();
    /** What the server last sent to clients, per slot (never saved). */
    final ItemStack[] synced = empty();
    /** Send everything next tick even if nothing changed (new object, so true after login/respawn). */
    boolean forceSync = true;
    /** The number of sets the clients were last told about. */
    int syncedSets = -1;

    public ExtraArmorInventory() {
        super(ExtraArmor.COUNT);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < ExtraArmor.COUNT;
    }

    /** Saves from older versions (or other limits) can hold a different number of slots; keep what fits. */
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        if (stacks.size() != ExtraArmor.COUNT) {
            NonNullList<ItemStack> resized = NonNullList.withSize(ExtraArmor.COUNT, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(stacks.size(), resized.size()); i++) resized.set(i, stacks.get(i));
            stacks = resized;
        }
    }

    /** Makes the next server tick resend all stacks to the owner and everyone tracking them. */
    public void resync() {
        forceSync = true;
    }

    private static ItemStack[] empty() {
        ItemStack[] stacks = new ItemStack[ExtraArmor.COUNT];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }
}
