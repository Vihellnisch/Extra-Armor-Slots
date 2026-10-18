package com.extraarmor.mixin.client;

import com.extraarmor.ExtraArmor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Jumping with an elytra in the extra chest slot opens it (the server side is WearableChecksMixin). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean extraarmor$extraElytra(ItemStack worn, LivingEntity entity, Operation<Boolean> original) {
        return original.call(worn, entity) || !ExtraArmor.prefer(entity, EquipmentSlot.CHEST, ItemStack.EMPTY, s -> original.call(s, entity)).isEmpty();
    }
}
