package com.extraarmor;

import com.extraarmor.network.ExtraArmorSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ExtraArmorMod.MODID)
public final class ExtraArmorEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        tickItems(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            ExtraArmorInventory inv = ExtraArmor.inventory(player);
            returnDisabledSets(player, inv);
            dropDuplicates(player, inv);
            applyChanges(player, inv);
            syncChanges(player, inv);
        }
    }

    /**
     * Inventory#tick gives worn armor inventoryTick with slot ids 36..39 (feet..head); many modded
     * armors run their effects there, so the extra column gets the same call with the same ids.
     */
    private static void tickItems(Player player) {
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        for (int i = 0; i < ExtraArmor.sets(player) * ExtraArmor.SLOTS.length; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) stack.inventoryTick(player.level(), player, 36 + ExtraArmor.SLOTS[i % 4].getIndex(), false);
        }
    }

    /** Sets the config switched off give their armor back (inventory, or dropped at the feet if full). */
    private static void returnDisabledSets(ServerPlayer player, ExtraArmorInventory inv) {
        for (int i = ExtraArmor.sets(player) * ExtraArmor.SLOTS.length; i < ExtraArmor.COUNT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            inv.setStackInSlot(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }

    /**
     * Other mods may move a worn piece they got from the extra column without telling it (read it, then
     * put the same stack somewhere else). The same stack object can never legitimately be in two places,
     * so if it shows up in the inventory or on the cursor, it left the extra slot.
     */
    private static void dropDuplicates(ServerPlayer player, ExtraArmorInventory inv) {
        for (int i = 0; i < ExtraArmor.COUNT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            boolean elsewhere = stack == player.containerMenu.getCarried() || stack == player.getOffhandItem();
            for (ItemStack other : player.getInventory().items) elsewhere |= other == stack;
            for (EquipmentSlot slot : ExtraArmor.SLOTS) elsewhere |= player.getItemBySlot(slot) == stack;
            if (elsewhere) inv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    /**
     * Mirrors LivingEntity#collectEquipmentChanges for the extra column: swaps attribute modifiers and
     * location effects. While the normal slot and the earlier sets' slot are empty, this piece is what
     * other mods see as worn, so they also get the usual LivingEquipmentChangeEvent for it.
     */
    private static void applyChanges(ServerPlayer player, ExtraArmorInventory inv) {
        for (int i = 0; i < ExtraArmor.COUNT; i++) {
            ItemStack now = inv.getStackInSlot(i);
            ItemStack before = inv.applied[i];
            if (ItemStack.matches(now, before)) continue;
            EquipmentSlot slot = ExtraArmor.SLOTS[i % 4];
            int set = i / 4;
            if (!before.isEmpty()) {
                before.forEachModifier(slot, (attribute, modifier) -> {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance != null) instance.removeModifier(ExtraArmor.modifierId(set, slot, modifier.id()));
                });
                EnchantmentHelper.stopLocationBasedEffects(before, player, slot);
            }
            if (!now.isEmpty()) {
                now.forEachModifier(slot, (attribute, modifier) -> {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance != null) {
                        instance.addOrUpdateTransientModifier(new AttributeModifier(
                                ExtraArmor.modifierId(set, slot, modifier.id()), modifier.amount(), modifier.operation()));
                    }
                });
                EnchantmentHelper.runLocationChangedEffects(player.serverLevel(), now, player, slot);
            }
            boolean frontmost = player.getItemBySlot(slot).isEmpty();
            for (int earlier = 0; earlier < set && frontmost; earlier++) frontmost = inv.getStackInSlot(ExtraArmor.index(earlier, slot)).isEmpty();
            if (frontmost) {
                NeoForge.EVENT_BUS.post(new LivingEquipmentChangeEvent(player, slot, before, now));
            }
            inv.applied[i] = now.copy();
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void syncChanges(ServerPlayer player, ExtraArmorInventory inv) {
        boolean changed = inv.forceSync || inv.syncedSets != ExtraArmor.sets(player);
        for (int i = 0; i < ExtraArmor.COUNT && !changed; i++) {
            changed = !ItemStack.matches(inv.getStackInSlot(i), inv.synced[i]);
        }
        if (!changed) return;
        inv.forceSync = false;
        inv.syncedSets = ExtraArmor.sets(player);
        for (int i = 0; i < ExtraArmor.COUNT; i++) inv.synced[i] = inv.getStackInSlot(i).copy();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, ExtraArmorSyncPayload.of(player));
    }

    /** A carved pumpkin (or modded ender mask) in the extra head slot keeps endermen calm. */
    @SubscribeEvent
    public static void onEnderManAnger(EnderManAngerEvent event) {
        Player player = event.getPlayer();
        if (!ExtraArmor.prefer(player, EquipmentSlot.HEAD, ItemStack.EMPTY, s -> s.isEnderMask(player, event.getEntity())).isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer watcher) {
            PacketDistributor.sendToPlayer(watcher, ExtraArmorSyncPayload.of(target));
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ExtraArmor.inventory(event.getEntity()).resync();
    }

    /** Extra armor drops on death like normal armor (curse of vanishing destroys it), unless keepInventory is on. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        for (int i = 0; i < ExtraArmor.COUNT; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            inv.setStackInSlot(i, ItemStack.EMPTY);
            if (stack.isEmpty() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;
            ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3D, player.getZ(), stack);
            item.setPickUpDelay(40);
            float speed = player.getRandom().nextFloat() * 0.5F;
            float angle = player.getRandom().nextFloat() * Mth.TWO_PI;
            item.setDeltaMovement(-Mth.sin(angle) * speed, 0.2D, Mth.cos(angle) * speed);
            event.getDrops().add(item);
        }
    }

    /** Called from LivingEntity#doHurtEquipment: extra armor wears down exactly like the normal armor. */
    public static void hurtArmor(Player player, DamageSource source, float damage, EquipmentSlot[] slots) {
        if (damage <= 0.0F || !(player.level() instanceof ServerLevel level)) return;
        int amount = (int) Math.max(1.0F, damage / 4.0F);
        for (EquipmentSlot slot : slots) {
            for (int set = 0; set < ExtraArmor.sets(player); set++) {
                ItemStack stack = ExtraArmor.get(player, slot, set);
                if (stack.getItem() instanceof ArmorItem && stack.canBeHurtBy(source)) {
                    stack.hurtAndBreak(amount, level, player, item -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, player.getSoundSource(), 0.8F, 0.8F + level.random.nextFloat() * 0.4F));
                }
            }
        }
    }

    private ExtraArmorEvents() {}
}
