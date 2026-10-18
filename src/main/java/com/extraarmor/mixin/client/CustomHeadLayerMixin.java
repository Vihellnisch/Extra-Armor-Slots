package com.extraarmor.mixin.client;

import com.extraarmor.ExtraArmor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Heads, pumpkins and other non-armor helmets in the extra head slot show when the normal head slot is empty. */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @ModifyExpressionValue(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack extraarmor$showExtraArmor(ItemStack worn, @Local(argsOnly = true) LivingEntity entity) {
        return ExtraArmor.visible(entity, EquipmentSlot.HEAD, worn);
    }
}
