package com.extraarmor.mixin;

import com.extraarmor.ExtraArmorList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Installs {@link ExtraArmorList} as the player's armor list (items, armor, offhand: the second withSize call). */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;", ordinal = 1))
    private NonNullList<ItemStack> extraarmor$armorList(NonNullList<ItemStack> vanilla) {
        return new ExtraArmorList((Inventory) (Object) this);
    }
}
