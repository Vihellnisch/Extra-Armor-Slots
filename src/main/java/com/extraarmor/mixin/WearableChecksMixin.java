package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A turtle shell or elytra in the extra column works like a worn one (vanilla only looks at the normal slot). */
@Mixin(Player.class)
public abstract class WearableChecksMixin {
    private static final String GET_ITEM_BY_SLOT =
            "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;";

    @ModifyExpressionValue(method = "turtleHelmetTick", at = @At(value = "INVOKE", target = GET_ITEM_BY_SLOT))
    private ItemStack extraarmor$extraTurtleShell(ItemStack worn) {
        return ExtraArmor.prefer((Player) (Object) this, EquipmentSlot.HEAD, worn, s -> s.is(Items.TURTLE_HELMET));
    }

    @ModifyExpressionValue(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = GET_ITEM_BY_SLOT))
    private ItemStack extraarmor$extraElytraStart(ItemStack worn) {
        Player self = (Player) (Object) this;
        return ExtraArmor.prefer(self, EquipmentSlot.CHEST, worn, s -> s.canElytraFly(self));
    }
}
