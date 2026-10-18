package com.extraarmor.mixin.client;

import com.extraarmor.ExtraArmorSlot;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The creative inventory tab wraps the player's menu slots and lays them out by index; vanilla would
 * put slots 46..49 under the hotbar. Keep the extra slots in their column on the left edge instead.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public abstract class CreativeSlotWrapperMixin {

    @ModifyArg(method = "<init>", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"))
    private static int extraarmor$x(int x, @Local(argsOnly = true) Slot target) {
        return target instanceof ExtraArmorSlot ? target.x : x;
    }

    @ModifyArg(method = "<init>", index = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"))
    private static int extraarmor$y(int y, @Local(argsOnly = true) Slot target) {
        return target instanceof ExtraArmorSlot ? target.y : y;
    }
}
