package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends the extra armor slots to the player's inventory menu (menu slots 46 onwards, after the
 * offhand, so every vanilla slot index stays the same). There are always slots for the maximum number
 * of sets, so client and server menus match whatever the config says; switched-off sets just stay
 * hidden and refuse items. Also teaches shift-click about them.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    @Unique
    private static final int EXTRA_START = 46, INV_START = 9, INV_END = 45;

    protected InventoryMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void extraarmor$addSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        for (int i = 0; i < ExtraArmor.COUNT; i++) {
            addSlot(new ExtraArmorSlot(owner, i));
        }
    }

    /**
     * Shift-click: out of an extra slot goes to the inventory; armor from the inventory goes to the
     * normal slot first (vanilla), then to the first enabled extra set with that slot free.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void extraarmor$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= slots.size()) return;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        int target = -1;
        if (slot instanceof ExtraArmorSlot) {
            target = -1;
        } else if (index >= INV_START && index < INV_END) {
            EquipmentSlot type = player.getEquipmentSlotForItem(stack);
            if (ExtraArmor.index(type) < 0 || type.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;
            if (!slots.get(8 - type.getIndex()).hasItem()) return; // vanilla puts it in the normal slot
            for (int set = 0; set < ExtraArmor.sets(player) && target < 0; set++) {
                Slot extra = slots.get(EXTRA_START + ExtraArmor.index(set, type));
                if (!extra.hasItem() && extra.mayPlace(stack)) target = EXTRA_START + ExtraArmor.index(set, type);
            }
            if (target < 0) return;
        } else {
            return;
        }
        ItemStack original = stack.copy();
        boolean moved = target < 0
                ? moveItemStackTo(stack, INV_START, INV_END, false)
                : moveItemStackTo(stack, target, target + 1, false);
        if (!moved) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, stack);
        cir.setReturnValue(original);
    }
}
