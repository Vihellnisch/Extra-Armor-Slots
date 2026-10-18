package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorCaps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.extraarmor.ExtraArmorEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hits that wear down armor (hurtArmor / hurtHelmet) wear down the extra armor too, and players'
 * protection enchantments use the raised cap (see {@link ExtraArmorCaps}). Vanilla "wearing X" checks
 * (leather freeze immunity, mob heads hiding you, elytra flight) count the extra column too.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "doHurtEquipment", at = @At("HEAD"))
    private void extraarmor$hurtExtraArmor(DamageSource source, float damage, EquipmentSlot[] slots, CallbackInfo ci) {
        if ((Object) this instanceof Player player) ExtraArmorEvents.hurtArmor(player, source, damage, slots);
    }

    @WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float extraarmor$doubledProtectionCap(float damage, float protection, Operation<Float> original) {
        if (!((Object) this instanceof Player) || !ExtraArmorCaps.active()) return original.call(damage, protection);
        return damage * (1.0F - ExtraArmorCaps.reduction(Mth.clamp(protection, 0.0F, ExtraArmorCaps.cap())));
    }

    @Inject(method = "canFreeze", at = @At("RETURN"), cancellable = true)
    private void extraarmor$extraFreezeImmunity(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cir.getReturnValueZ() && ExtraArmor.anyExtra(self, s -> s.is(ItemTags.FREEZE_IMMUNE_WEARABLES))) cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "getVisibilityPercent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack extraarmor$extraMobHead(ItemStack worn) {
        return ExtraArmor.prefer((LivingEntity) (Object) this, EquipmentSlot.HEAD, worn,
                s -> s.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock);
    }

    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack extraarmor$extraElytraFlight(ItemStack worn) {
        LivingEntity self = (LivingEntity) (Object) this;
        return ExtraArmor.prefer(self, EquipmentSlot.CHEST, worn, s -> s.canElytraFly(self));
    }
}
