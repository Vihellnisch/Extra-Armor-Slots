package com.extraarmor.mixin.client;

import com.extraarmor.ExtraArmor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Armor pieces render from the extra column when the normal slot is empty (normal armor wins when both are worn). */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @ModifyExpressionValue(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack extraarmor$showExtraArmor(ItemStack worn, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) EquipmentSlot slot) {
        return ExtraArmor.visible(entity, slot, worn);
    }
}
