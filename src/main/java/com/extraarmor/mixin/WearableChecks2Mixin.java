package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gold (or any piglin-neutral modded armor) in the extra column keeps piglins calm. */
@Mixin(PiglinAi.class)
public abstract class WearableChecks2Mixin {

    @Inject(method = "isWearingGold", at = @At("RETURN"), cancellable = true)
    private static void extraarmor$extraGold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ExtraArmor.anyExtra(entity, s -> s.makesPiglinsNeutral(entity))) cir.setReturnValue(true);
    }
}
