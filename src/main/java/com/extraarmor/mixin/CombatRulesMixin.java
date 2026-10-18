package com.extraarmor.mixin;

import com.extraarmor.ExtraArmorCaps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players' armor uses the raised cap (see {@link ExtraArmorCaps}); identical to vanilla up to 20 points. */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {

    @Inject(method = "getDamageAfterAbsorb", at = @At("HEAD"), cancellable = true)
    private static void extraarmor$doubledArmorCap(LivingEntity entity, float damage, DamageSource source, float armor, float toughness,
                                                CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Player) || !ExtraArmorCaps.active()) return;
        float toughnessFactor = 2.0F + toughness / 4.0F;
        float effective = Mth.clamp(armor - damage / toughnessFactor, armor * 0.2F, ExtraArmorCaps.cap());
        float blocked = ExtraArmorCaps.reduction(effective);
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && entity.level() instanceof ServerLevel level) {
            // the Breach enchantment lowers armor effectiveness, same as vanilla
            blocked = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness(level, weapon, entity, source, blocked), 0.0F, 1.0F);
        }
        cir.setReturnValue(damage * (1.0F - blocked));
    }
}
