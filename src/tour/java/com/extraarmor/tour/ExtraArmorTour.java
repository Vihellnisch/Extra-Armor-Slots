package com.extraarmor.tour;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorCaps;
import com.extraarmor.ExtraArmorConfig;
import com.extraarmor.ExtraArmorInventory;
import com.extraarmor.ExtraArmorMod;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Dev-only scripted checks (./gradlew runTourClient). Each stop sets up a scene and logs "TOUR CHECK"
 * lines; ExtraArmorTourClient screenshots each stop and quits after the last. Off unless -Dextraarmor.tour=true.
 */
@EventBusSubscriber(modid = ExtraArmorMod.MODID)
public final class ExtraArmorTour {
    static final boolean ENABLED = Boolean.getBoolean("extraarmor.tour");
    static final boolean SHOWCASE = Boolean.getBoolean("extraarmor.tour.showcase");
    static final int FIRST_STOP = 60;
    static final int STOP_LENGTH = 100;
    static final int SHOT_AT = 85;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos C = new BlockPos(3000, 150, 3000);

    private record Stop(String name, BiConsumer<ServerPlayer, ServerLevel> action) {
    }

    private static final List<Stop> STOPS = buildStops();
    static final List<String> STOP_NAMES = STOPS.stream().map(Stop::name).toList();
    static volatile int ticks = -1;
    private static final List<Map.Entry<Integer, Runnable>> LATER = new ArrayList<>();
    private static int passed, failed;

    private static List<Stop> buildStops() {
        List<Stop> s = new ArrayList<>();
        s.add(new Stop("stats", (p, l) -> {
            wear(p, l, "diamond", null);
            later(5, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR), tough = p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                check("normal diamond only: armor 20", armor == 20.0D, armor);
                check("normal diamond only: toughness 8", tough == 8.0D, tough);
                wear(p, l, "diamond", "netherite");
            });
            later(12, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR), tough = p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                double kb = p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                check("diamond + extra netherite: armor 20+20 (cap now 60)", armor == 40.0D, armor);
                check("diamond + extra netherite: toughness 8+12", tough == 20.0D, tough);
                check("extra netherite knockback resistance 0.4", Math.abs(kb - 0.4D) < 1e-6, kb);
                check("armor bar value", p.getArmorValue() == 40, p.getArmorValue());
                float prot = EnchantmentHelper.getDamageProtection(l, p, l.damageSources().generic());
                check("protection IV on extra boots counts (4)", prot == 4.0F, prot);
                double oxygen = p.getAttributeValue(Attributes.OXYGEN_BONUS);
                check("respiration III on extra helmet (oxygen bonus 3)", oxygen == 3.0D, oxygen);
                ExtraArmor.inventory(p).setStackInSlot(0, ItemStack.EMPTY);
            });
            later(18, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR);
                check("taking the extra helmet off removes its armor (20+17)", armor == 37.0D, armor);
                double oxygen = p.getAttributeValue(Attributes.OXYGEN_BONUS);
                check("taking the extra helmet off removes respiration", oxygen == 0.0D, oxygen);
                wear(p, l, null, "netherite");
            });
            later(24, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR);
                check("extra netherite alone: armor 20", armor == 20.0D, armor);
                p.setGameMode(GameType.SURVIVAL);
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                ItemStack chest = ExtraArmor.inventory(p).getStackInSlot(1);
                ItemStack normalChest = p.getItemBySlot(EquipmentSlot.CHEST);
                int before = chest.getDamageValue();
                p.invulnerableTime = 0;
                p.hurt(l.damageSources().cactus(), 10.0F); // generic damage bypasses armor
                check("a hit wears the extra chestplate down (10 dmg -> 2)", chest.getDamageValue() - before == 2, chest.getDamageValue() - before);
                check("...and the normal chestplate too", normalChest.getDamageValue() == 2, normalChest.getDamageValue());
                p.setHealth(p.getMaxHealth());
                p.setGameMode(GameType.CREATIVE);
            });
            look(p);
        }));
        s.add(new Stop("doubled_caps", (p, l) -> {
            wear(p, l, "netherite", "netherite");
            for (EquipmentSlot slot : ExtraArmor.SLOTS) {
                p.getItemBySlot(slot).enchant(ench(l, Enchantments.PROTECTION), 4);
                ExtraArmor.get(p, slot, 0).enchant(ench(l, Enchantments.PROTECTION), 4);
            }
            later(5, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR), tough = p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                check("double netherite: armor 40", armor == 40.0D, armor);
                check("double netherite: toughness 24 (cap now 40)", tough == 24.0D, tough);
                var cactus = l.damageSources().cactus();
                float vanillaCase = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p, 10.0F, cactus, 20.0F, 8.0F);
                check("20 armor / 8 toughness still exactly vanilla (10 -> 3)", Math.abs(vanillaCase - 3.0F) < 1e-4, vanillaCase);
                float doubled = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p, 10.0F, cactus, 40.0F, 24.0F);
                check("40 armor / 24 toughness blocks 95% (10 -> 0.5)", Math.abs(doubled - 0.5F) < 1e-4, doubled);
                float prot = EnchantmentHelper.getDamageProtection(l, p, cactus);
                check("protection IV on all 8 pieces = 32", prot == 32.0F, prot);
                p.setGameMode(GameType.SURVIVAL);
                p.setHealth(p.getMaxHealth());
                p.invulnerableTime = 0;
                float before = p.getHealth();
                p.hurt(cactus, 10.0F);
                float lost = before - p.getHealth();
                // armor 10 -> 0.5, then protection 32: 1 - 0.2 * (1 - 12/25) = 89.6% blocked -> 0.052
                check("real hit: 10 damage -> ~0.052 through both layers", Math.abs(lost - 0.052F) < 0.005F, lost);
                p.setHealth(p.getMaxHealth());
                p.setGameMode(GameType.CREATIVE);
            });
            look(p);
        }));
        s.add(new Stop("mod_set_bonus", (p, l) -> {
            // Orbital Strike (run-tour/mods) checks its full set with player.getItemBySlot(...)
            wear(p, l, "diamond", null);
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            for (int i = 0; i < 4; i++) inv.setStackInSlot(i, helldivers(ExtraArmor.SLOTS[i]));
            inv.getStackInSlot(0).enchant(ench(l, Enchantments.PROTECTION), 4);
            check("Orbital Strike sees a full set worn in the extra column", orbitalFullSet(p), "");
            check("vanilla still sees the normal diamond helmet", p.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), p.getItemBySlot(EquipmentSlot.HEAD));
            float prot = EnchantmentHelper.getDamageProtection(l, p, l.damageSources().cactus());
            check("extra enchantments still counted once (protection 4)", prot == 4.0F, prot);
            // split set: helmet + boots normal, chest + legs extra, diamond in the other slots
            p.setItemSlot(EquipmentSlot.HEAD, helldivers(EquipmentSlot.HEAD));
            p.setItemSlot(EquipmentSlot.FEET, helldivers(EquipmentSlot.FEET));
            inv.setStackInSlot(0, new ItemStack(Items.DIAMOND_HELMET));
            inv.setStackInSlot(3, ItemStack.EMPTY);
            check("Orbital Strike sees a set split across both columns", orbitalFullSet(p), "");
            for (int i = 0; i < 4; i++) inv.setStackInSlot(i, ItemStack.EMPTY);
            check("...and no set without the extra pieces", !orbitalFullSet(p), "");
            for (int i = 0; i < 4; i++) inv.setStackInSlot(i, helldivers(ExtraArmor.SLOTS[i]));
            wear(p, l, null, null);
            for (int i = 0; i < 4; i++) inv.setStackInSlot(i, helldivers(ExtraArmor.SLOTS[i]));
            look(p);
        }));
        s.add(new Stop("any_mod_style", (p, l) -> {
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            wear(p, l, null, null);
            inv.setStackInSlot(0, new ItemStack(Items.NETHERITE_HELMET));
            testmod.ArmorCheckStyles.helmetSeenByEachStyle(p).forEach((style, item) ->
                    check("another mod's " + style + " sees the extra helmet (normal slot empty)", item == Items.NETHERITE_HELMET, item));
            check("vanilla still sees an empty head slot", p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), p.getItemBySlot(EquipmentSlot.HEAD));
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            testmod.ArmorCheckStyles.helmetSeenByEachStyle(p).forEach((style, item) ->
                    check("another mod's " + style + " sees the normal helmet when both are worn", item == Items.DIAMOND_HELMET, item));
            // writes follow reads: "unequip" in another mod takes off the piece it sees
            p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            p.getInventory().clearContent();
            testmod.ArmorCheckStyles.unequipHelmet(p);
            check("another mod unequipping takes the extra helmet off", inv.getStackInSlot(0).isEmpty(), inv.getStackInSlot(0));
            check("...into the inventory, exactly once", p.getInventory().countItem(Items.NETHERITE_HELMET) == 1, p.getInventory().countItem(Items.NETHERITE_HELMET));
            p.getInventory().clearContent();
            inv.setStackInSlot(0, new ItemStack(Items.NETHERITE_HELMET));
            testmod.ArmorCheckStyles.sloppyMoveHelmet(p);
            later(3, () -> {
                int total = p.getInventory().countItem(Items.NETHERITE_HELMET) + inv.getStackInSlot(0).getCount();
                check("a sloppy move can not duplicate the extra helmet", total == 1, total);
                p.getInventory().clearContent();
                // vanilla wearables from the extra column, with normal armor on
                wear(p, l, "diamond", null);
                inv.setStackInSlot(0, new ItemStack(Items.TURTLE_HELMET));
                inv.setStackInSlot(1, new ItemStack(Items.ELYTRA));
                inv.setStackInSlot(3, new ItemStack(Items.LEATHER_BOOTS));
                inv.setStackInSlot(2, new ItemStack(Items.GOLDEN_LEGGINGS));
                p.removeAllEffects();
            });
            later(8, () -> {
                check("turtle shell in the extra column gives water breathing", p.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING), p.getActiveEffects());
                check("gold in the extra column calms piglins", net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingGold(p), "");
                check("leather boots in the extra column walk on powder snow", net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(p), "");
                check("leather in the extra column stops freezing", !p.canFreeze(), p.canFreeze());
                p.setGameMode(GameType.SURVIVAL);
                p.teleportTo(p.getX(), p.getY() + 20, p.getZ());
                p.setOnGround(false);
                boolean flying = p.tryToStartFallFlying();
                check("elytra in the extra column flies over a diamond chestplate", flying && p.isFallFlying(), flying);
                p.stopFallFlying();
                p.setGameMode(GameType.CREATIVE);
                var enderman = net.minecraft.world.entity.EntityType.ENDERMAN.create(l);
                inv.setStackInSlot(0, new ItemStack(net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN));
                boolean calm = net.neoforged.neoforge.common.CommonHooks.shouldSuppressEnderManAnger(enderman, p, p.getItemBySlot(EquipmentSlot.HEAD));
                check("carved pumpkin in the extra column calms endermen", calm, calm);
                wear(p, l, null, null);
                look(p);
            });
            look(p);
        }));
        // real input: the client clicks / presses keys (ExtraArmorTourFlows), the server checks what arrived
        s.add(new Stop("creative_place", (p, l) -> {
            // what loading a saved player does: NeoForge replaces the attachment object after the menu exists
            p.setData(ExtraArmorMod.EXTRA_ARMOR, new ExtraArmorInventory());
            wear(p, l, null, null);
            p.getInventory().clearContent();
            look(p);
            later(70, () -> {
                checkExtraHelldivers(p, "creative inventory clicks");
                check("Orbital Strike (server) sees the set placed in creative", orbitalFullSet(p), "");
            });
        }));
        s.add(new Stop("survival_place", (p, l) -> {
            // what loading a saved player does: NeoForge replaces the attachment object after the menu exists
            p.setData(ExtraArmorMod.EXTRA_ARMOR, new ExtraArmorInventory());
            wear(p, l, "diamond", null);
            p.getInventory().clearContent();
            for (int i = 0; i < 4; i++) p.getInventory().setItem(9 + i, helldivers(ExtraArmor.SLOTS[i]));
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            later(70, () -> {
                checkExtraHelldivers(p, "survival inventory clicks");
                check("Orbital Strike (server) sees the set placed in survival", orbitalFullSet(p), "");
                p.setGameMode(GameType.CREATIVE);
            });
        }));
        s.add(new Stop("stratagem_keys", (p, l) -> {
            wear(p, l, "diamond", null);
            p.getInventory().clearContent();
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            for (int i = 0; i < 4; i++) inv.setStackInSlot(i, helldivers(ExtraArmor.SLOTS[i]));
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            later(70, () -> {
                long beacons = p.getInventory().items.stream()
                        .filter(st -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getNamespace().equals("orbitalstrike")).count();
                check("pressing G, the code, G again gives a stratagem beacon", beacons > 0, beacons);
                p.setGameMode(GameType.CREATIVE);
            });
        }));
        s.add(new Stop("elytra_glide", (p, l) -> {
            wear(p, l, "diamond", null);
            ExtraArmor.inventory(p).setStackInSlot(1, new ItemStack(Items.ELYTRA));
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 4));
            p.teleportTo(p.getX(), p.getY() + 60, p.getZ());
            for (int t : new int[]{30, 45, 60}) {
                later(t, () -> LOGGER.info("TOUR flow elytra +{}: server fall flying={} y={}", t, p.isFallFlying(), String.format("%.1f", p.getY())));
            }
            later(60, () -> {
                check("jumping with an extra elytra keeps gliding (server, 35 ticks later)", p.isFallFlying(), p.isFallFlying());
                p.setGameMode(GameType.CREATIVE);
                look(p);
            });
        }));
        s.add(new Stop("elytra_wings_back", (p, l) -> {
            // showcase: wings from the extra chest slot over a normal diamond chestplate
            wear(p, l, "diamond", null);
            ExtraArmor.inventory(p).setStackInSlot(1, new ItemStack(Items.ELYTRA));
            look(p);
        }));
        s.add(new Stop("shift_click", (p, l) -> {
            wear(p, l, "diamond", null);
            p.getInventory().clearContent();
            wear(p, l, "diamond", null);
            p.getInventory().setItem(9, new ItemStack(Items.NETHERITE_HELMET));
            int menuIndex = 9; // inventory slot 9 is menu slot 9
            p.inventoryMenu.quickMoveStack(p, menuIndex);
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            check("shift-click helmet with normal helmet worn -> extra slot", inv.getStackInSlot(0).is(Items.NETHERITE_HELMET), inv.getStackInSlot(0));
            p.inventoryMenu.quickMoveStack(p, 46);
            check("shift-click out of the extra slot -> inventory", inv.getStackInSlot(0).isEmpty() && p.getInventory().contains(new ItemStack(Items.NETHERITE_HELMET)), inv.getStackInSlot(0));
            p.getInventory().clearContent();
            look(p);
        }));
        s.add(new Stop("inventory_screen", (p, l) -> {
            wear(p, l, "diamond", "netherite");
            dressInventory(p);
            look(p);
            p.setGameMode(GameType.SURVIVAL); // in creative, E opens the creative inventory instead
        }));
        s.add(new Stop("creative_screen", (p, l) -> look(p)));
        s.add(new Stop("recipe_book_screen", (p, l) -> {
            look(p);
            p.setGameMode(GameType.SURVIVAL);
        }));
        s.add(new Stop("extra_only_front", (p, l) -> {
            wear(p, l, null, "netherite");
            look(p);
        }));
        s.add(new Stop("both_front", (p, l) -> {
            wear(p, l, "diamond", "netherite");
            look(p);
        }));
        s.add(new Stop("mixed_front", (p, l) -> {
            wear(p, l, null, "netherite");
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            look(p);
        }));
        s.add(new Stop("three_sets_screen", (p, l) -> {
            sets(3);
            p.getInventory().clearContent(); // clears worn armor too, so before putting armor on
            wear(p, l, "diamond", "netherite");
            dressInventory(p);
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            for (EquipmentSlot slot : ExtraArmor.SLOTS) {
                inv.setStackInSlot(ExtraArmor.index(1, slot), piece("iron", slot));
                inv.setStackInSlot(ExtraArmor.index(2, slot), piece("gold", slot));
            }
            inv.getStackInSlot(ExtraArmor.index(2, EquipmentSlot.FEET)).enchant(ench(l, Enchantments.PROTECTION), 4);
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            later(5, () -> {
                // diamond 20 + netherite 20 + iron 15 + gold 11; 3 extra sets -> armor cap 30 * 4
                double armor = p.getAttributeValue(Attributes.ARMOR);
                check("3 sets: diamond + netherite + iron + gold armor = 66", armor == 66.0D, armor);
                double tough = p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                check("3 sets: toughness 8 + 12", tough == 20.0D, tough);
                float prot = EnchantmentHelper.getDamageProtection(l, p, l.damageSources().cactus());
                check("3 sets: protection IV on set 1 and set 3 boots both count (8)", prot == 8.0F, prot);
                float dmg = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p, 10.0F, l.damageSources().cactus(), 66.0F, 20.0F);
                // effective 66 - 10/7 = 64.57 -> layers 20,20,20,4.57: 0.2^3 * (1 - 4.57/25) = 0.00654
                check("3 sets: 4 cap layers (10 dmg -> ~0.065)", Math.abs(dmg - 0.0654F) < 0.001F, dmg);
                net.minecraft.world.inventory.Slot set3 = p.inventoryMenu.slots.get(46 + ExtraArmor.index(2, EquipmentSlot.HEAD));
                net.minecraft.world.inventory.Slot set4 = p.inventoryMenu.slots.get(46 + ExtraArmor.index(3, EquipmentSlot.HEAD));
                check("3 sets: third set's slots take armor", set3.mayPlace(new ItemStack(Items.IRON_HELMET)), "");
                check("3 sets: fourth set's slots stay closed", !set4.mayPlace(new ItemStack(Items.IRON_HELMET)), "");
                check("menu always has slots for 10 sets (46 + 40)", p.inventoryMenu.slots.size() == 86, p.inventoryMenu.slots.size());
            });
        }));
        s.add(new Stop("mod_view_three_sets", (p, l) -> {
            // Helldivers helmet only in the third set, other mods' armor elsewhere: Orbital Strike still finds its full set
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            wear(p, l, "diamond", "netherite");
            for (int i = 0; i < ExtraArmor.COUNT; i++) if (i >= 4) inv.setStackInSlot(i, ItemStack.EMPTY);
            inv.setStackInSlot(ExtraArmor.index(2, EquipmentSlot.HEAD), helldivers(EquipmentSlot.HEAD));
            inv.setStackInSlot(ExtraArmor.index(1, EquipmentSlot.CHEST), helldivers(EquipmentSlot.CHEST));
            p.setItemSlot(EquipmentSlot.LEGS, helldivers(EquipmentSlot.LEGS));
            inv.setStackInSlot(ExtraArmor.index(2, EquipmentSlot.FEET), helldivers(EquipmentSlot.FEET));
            check("Orbital Strike finds its set spread over normal + 3 extra sets", orbitalFullSet(p), "");
            check("vanilla still sees the normal diamond helmet", p.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), p.getItemBySlot(EquipmentSlot.HEAD));
            look(p);
        }));
        s.add(new Stop("ten_sets_screen", (p, l) -> {
            sets(10);
            wear(p, l, "diamond", "netherite");
            ExtraArmorInventory inv = ExtraArmor.inventory(p);
            String[] mats = {"netherite", "iron", "gold", "chainmail", "leather", "diamond", "netherite", "iron", "gold", "chainmail"};
            for (int set = 0; set < 10; set++) {
                for (EquipmentSlot slot : ExtraArmor.SLOTS) inv.setStackInSlot(ExtraArmor.index(set, slot), piece(mats[set], slot));
            }
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            later(5, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR);
                check("10 sets: every set adds armor (well past the old 60)", armor > 100.0D, armor);
            });
        }));
        s.add(new Stop("fewer_sets", (p, l) -> {
            // ten sets worn, config back to 1: sets 2..10 come back into the inventory
            p.getInventory().clearContent();
            sets(1);
            later(5, () -> {
                ExtraArmorInventory inv = ExtraArmor.inventory(p);
                boolean emptied = true;
                for (int i = 4; i < ExtraArmor.COUNT; i++) emptied &= inv.getStackInSlot(i).isEmpty();
                check("lowering the config to 1 set empties sets 2..10", emptied, "");
                long returned = p.getInventory().items.stream().filter(st -> !st.isEmpty()).count();
                check("...and gives those 36 pieces back (inventory 36 + rest dropped)", returned == 36, returned);
                check("...the first extra set stays worn", inv.getStackInSlot(0).is(Items.NETHERITE_HELMET), inv.getStackInSlot(0));
                p.getInventory().clearContent();
                for (net.minecraft.world.entity.item.ItemEntity e : l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(p.blockPosition()).inflate(8))) e.discard();
            });
            look(p);
        }));
        s.add(new Stop("caps_off", (p, l) -> {
            ExtraArmorConfig.RAISED_CAPS.set(false);
            ExtraArmorCaps.applyAttributeCaps();
            wear(p, l, "netherite", "netherite");
            later(5, () -> {
                double armor = p.getAttributeValue(Attributes.ARMOR);
                check("caps off: armor back to the vanilla max 30", armor == 30.0D, armor);
                float dmg = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p, 10.0F, l.damageSources().cactus(), 40.0F, 24.0F);
                check("caps off: armor blocks at most 80% (10 -> 2)", Math.abs(dmg - 2.0F) < 1e-4, dmg);
                ExtraArmorConfig.RAISED_CAPS.set(true);
                ExtraArmorCaps.applyAttributeCaps();
            });
            look(p);
        }));
        s.add(new Stop("death_drop", (p, l) -> {
            wear(p, l, "diamond", "netherite");
            look(p);
            p.setGameMode(GameType.SURVIVAL);
            p.kill();
            later(5, () -> {
                long netherite = l.getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(8))
                        .stream().filter(e -> e.getItem().is(Items.NETHERITE_CHESTPLATE) || e.getItem().is(Items.NETHERITE_HELMET)
                                || e.getItem().is(Items.NETHERITE_LEGGINGS) || e.getItem().is(Items.NETHERITE_BOOTS)).count();
                check("extra armor drops on death", netherite == 4, netherite);
                check("extra slots empty after death", ExtraArmor.inventory(p).getStackInSlot(1).isEmpty(), ExtraArmor.inventory(p).getStackInSlot(1));
                LOGGER.info("TOUR CHECKS DONE: {} passed, {} failed", passed, failed);
            });
        }));
        String only = System.getProperty("extraarmor.tour.only", "");
        if (!only.isEmpty()) s.removeIf(stop -> !stop.name().startsWith(only));
        if (SHOWCASE) {
            List<String> keep = List.of("inventory_screen", "three_sets_screen", "ten_sets_screen", "creative_screen", "extra_only_front",
                    "both_front", "mixed_front", "elytra_wings_back");
            s.removeIf(stop -> !keep.contains(stop.name()));
        }
        return s;
    }

    private static void check(String what, boolean ok, Object value) {
        if (ok) passed++;
        else failed++;
        LOGGER.info("TOUR CHECK {} {} (got {})", ok ? "PASS" : "FAIL", what, value);
    }

    /** Puts a full set of the given materials in the normal / extra column (null = empty). */
    private static void wear(ServerPlayer p, ServerLevel l, String normal, String extra) {
        ExtraArmorInventory inv = ExtraArmor.inventory(p);
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = ExtraArmor.SLOTS[i];
            p.setItemSlot(slot, normal == null ? ItemStack.EMPTY : piece(normal, slot));
            ItemStack e = extra == null ? ItemStack.EMPTY : piece(extra, slot);
            if (slot == EquipmentSlot.HEAD && !e.isEmpty()) e.enchant(ench(l, Enchantments.RESPIRATION), 3);
            if (slot == EquipmentSlot.FEET && !e.isEmpty()) e.enchant(ench(l, Enchantments.PROTECTION), 4);
            inv.setStackInSlot(i, e);
        }
    }

    /** Showcase screenshots: a lived-in inventory instead of an empty grid. */
    private static void dressInventory(ServerPlayer p) {
        if (!SHOWCASE) return;
        Item[] items = {Items.NETHERITE_SWORD, Items.DIAMOND_PICKAXE, Items.BOW, Items.COOKED_BEEF, Items.TORCH, Items.ENDER_PEARL,
                Items.OAK_PLANKS, Items.GOLDEN_APPLE, Items.WATER_BUCKET};
        int[] counts = {1, 1, 1, 32, 64, 12, 64, 6, 1};
        for (int i = 0; i < items.length; i++) p.getInventory().setItem(i, new ItemStack(items[i], counts[i]));
        p.getInventory().setItem(9, new ItemStack(Items.ARROW, 64));
        p.getInventory().setItem(13, new ItemStack(Items.IRON_INGOT, 23));
        p.getInventory().setItem(17, new ItemStack(Items.SHIELD));
        p.getInventory().setItem(22, new ItemStack(Items.EXPERIENCE_BOTTLE, 16));
        p.getInventory().setItem(30, new ItemStack(Items.COBBLESTONE, 64));
        p.getInventory().selected = 0;
    }

    /** Changes the configured number of extra sets the way a config edit would. */
    private static void sets(int count) {
        ExtraArmorConfig.EXTRA_SETS.set(count);
        ExtraArmorCaps.applyAttributeCaps();
    }

    private static void checkExtraHelldivers(ServerPlayer p, String how) {
        ExtraArmorInventory inv = ExtraArmor.inventory(p);
        boolean all = true;
        for (int i = 0; i < 4; i++) all &= inv.getStackInSlot(i).getItem() == helldivers(ExtraArmor.SLOTS[i]).getItem();
        check("server has the Helldivers set in the extra column after " + how, all,
                java.util.stream.IntStream.range(0, 4).mapToObj(inv::getStackInSlot).toList());
    }

    static ItemStack helldivers(EquipmentSlot slot) {
        String piece = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            default -> "boots";
        };
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orbitalstrike", "helldivers_" + piece)));
    }

    private static boolean orbitalFullSet(ServerPlayer p) {
        try {
            Class<?> mod = Class.forName("com.orbitalstrike.OrbitalStrikeMod", true, ExtraArmorTour.class.getClassLoader());
            return (boolean) mod.getMethod("isWearingFullHelldiverSet", net.minecraft.world.entity.player.Player.class).invoke(null, p);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("TOUR could not call Orbital Strike", e);
            return false;
        }
    }

    private static net.minecraft.core.Holder<Enchantment> ench(ServerLevel l, ResourceKey<Enchantment> key) {
        return l.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    private static ItemStack piece(String material, EquipmentSlot slot) {
        Item item = switch (material + "_" + slot.getName()) {
            case "diamond_head" -> Items.DIAMOND_HELMET;
            case "diamond_chest" -> Items.DIAMOND_CHESTPLATE;
            case "diamond_legs" -> Items.DIAMOND_LEGGINGS;
            case "diamond_feet" -> Items.DIAMOND_BOOTS;
            case "iron_head" -> Items.IRON_HELMET;
            case "iron_chest" -> Items.IRON_CHESTPLATE;
            case "iron_legs" -> Items.IRON_LEGGINGS;
            case "iron_feet" -> Items.IRON_BOOTS;
            case "gold_head" -> Items.GOLDEN_HELMET;
            case "gold_chest" -> Items.GOLDEN_CHESTPLATE;
            case "gold_legs" -> Items.GOLDEN_LEGGINGS;
            case "gold_feet" -> Items.GOLDEN_BOOTS;
            case "chainmail_head" -> Items.CHAINMAIL_HELMET;
            case "chainmail_chest" -> Items.CHAINMAIL_CHESTPLATE;
            case "chainmail_legs" -> Items.CHAINMAIL_LEGGINGS;
            case "chainmail_feet" -> Items.CHAINMAIL_BOOTS;
            case "leather_head" -> Items.LEATHER_HELMET;
            case "leather_chest" -> Items.LEATHER_CHESTPLATE;
            case "leather_legs" -> Items.LEATHER_LEGGINGS;
            case "leather_feet" -> Items.LEATHER_BOOTS;
            case "netherite_head" -> Items.NETHERITE_HELMET;
            case "netherite_chest" -> Items.NETHERITE_CHESTPLATE;
            case "netherite_legs" -> Items.NETHERITE_LEGGINGS;
            default -> Items.NETHERITE_BOOTS;
        };
        return new ItemStack(item);
    }

    /** Stands the player on a grass platform facing south (+Z), towards a stone wall. */
    private static void look(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) l.setBlockAndUpdate(C.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.teleportTo(l, C.getX() + 0.5D, C.getY() + 1.0D, C.getZ() + 0.5D, 180.0F, 10.0F);
        p.setYHeadRot(180.0F);
        p.setYBodyRot(180.0F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(0);
        ServerLevel level = event.getServer().overworld();
        if (++ticks == 0) {
            GameRules rules = level.getGameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, event.getServer());
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, event.getServer());
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            rules.getRule(GameRules.RULE_KEEPINVENTORY).set(false, event.getServer());
            rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(false, event.getServer());
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
        }
        LATER.removeIf(task -> {
            if (task.getKey() > ticks) return false;
            try {
                task.getValue().run();
            } catch (RuntimeException e) {
                LOGGER.error("TOUR delayed task failed", e);
            }
            return true;
        });
        if (ticks < FIRST_STOP || (ticks - FIRST_STOP) % STOP_LENGTH != 0) return;
        int stop = (ticks - FIRST_STOP) / STOP_LENGTH;
        if (stop >= STOPS.size()) return;
        player.setGameMode(GameType.CREATIVE);
        if (SHOWCASE) player.removeAllEffects();
        try {
            STOPS.get(stop).action().accept(player, level);
        } catch (RuntimeException e) {
            LOGGER.error("TOUR stop {} failed", STOPS.get(stop).name(), e);
        }
        LOGGER.info("TOUR stop {}: {} at {}", stop, STOPS.get(stop).name(), player.position());
    }

    private static void later(int delay, Runnable task) {
        LATER.add(Map.entry(ticks + delay, task));
    }

    private ExtraArmorTour() {}
}
