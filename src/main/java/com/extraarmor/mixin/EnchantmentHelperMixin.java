package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorInventory;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Every "what enchantments is this entity wearing" loop (protection, thorns, frost walker, ticking
 * effects...) goes through runIterationOnEquipment, so the extra sets join it there. Mending picks
 * its item through getRandomItemWith, which gets the extra column too.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "runIterationOnEquipment", at = @At("TAIL"))
    private static void extraarmor$visitExtraArmor(LivingEntity entity, EnchantmentHelper.EnchantmentInSlotVisitor visitor, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        for (int i = 0; i < ExtraArmor.sets(player) * ExtraArmor.SLOTS.length; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) EnchantmentHelper.runIterationOnItem(stack, ExtraArmor.SLOTS[i % 4], entity, visitor);
        }
    }

    @Inject(method = "getRandomItemWith", at = @At("HEAD"), cancellable = true)
    private static void extraarmor$randomItemWithExtra(DataComponentType<?> component, LivingEntity entity, Predicate<ItemStack> filter,
                                                     CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir) {
        if (!(entity instanceof Player player)) return;
        List<EnchantedItemInUse> list = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            extraarmor$collect(list, entity.getItemBySlot(slot), slot, component, entity, filter);
        }
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        for (int i = 0; i < ExtraArmor.sets(player) * ExtraArmor.SLOTS.length; i++) {
            extraarmor$collect(list, inv.getStackInSlot(i), ExtraArmor.SLOTS[i % 4], component, entity, filter);
        }
        cir.setReturnValue(Util.getRandomSafe(list, entity.getRandom()));
    }

    private static void extraarmor$collect(List<EnchantedItemInUse> list, ItemStack stack, EquipmentSlot slot, DataComponentType<?> component,
                                           LivingEntity entity, Predicate<ItemStack> filter) {
        if (!filter.test(stack)) return;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.value().effects().has(component) && holder.value().matchingSlot(slot)) {
                list.add(new EnchantedItemInUse(stack, slot, entity));
            }
        }
    }
}
