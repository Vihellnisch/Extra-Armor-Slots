package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leather boots (or modded snow boots) in the extra column let you walk on powder snow. */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
    private static void extraarmor$extraSnowBoots(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !(entity instanceof LivingEntity living)) return;
        if (!ExtraArmor.prefer(living, EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY, s -> s.canWalkOnPowderedSnow(living)).isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
