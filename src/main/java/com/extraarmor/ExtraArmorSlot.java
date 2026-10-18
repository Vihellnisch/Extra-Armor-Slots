package com.extraarmor;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

/** One extra armor slot in the player's inventory menu; behaves like the vanilla armor slot next to it. */
public final class ExtraArmorSlot extends SlotItemHandler {
    /** Inventory-screen x of the extra column (18 px left of the vanilla column at x=8). */
    public static final int X = 8 - 18;

    /** Set by the client while the extra columns aren't drawn (recipe book open), so the slots hide too. */
    public static volatile boolean hiddenOnClient;
    /** Client screen layout: whether extra set c is on the page shown right now (see ExtraArmorClient). */
    public static volatile java.util.function.IntPredicate shownOnClient = set -> true;

    private final Player owner;
    private final EquipmentSlot slot;
    private final int set;

    /** {@code index}: storage index (set * 4 + piece); starts in its column left of the vanilla armor. */
    public ExtraArmorSlot(Player owner, int index) {
        super(new LiveInventory(owner), index, X - 18 * (index / 4), 8 + (index % 4) * 18);
        this.owner = owner;
        this.slot = ExtraArmor.SLOTS[index % 4];
        this.set = index / 4;
    }

    public EquipmentSlot equipmentSlot() {
        return slot;
    }

    public int set() {
        return set;
    }

    /** Whether the config currently allows this slot's set. */
    public boolean enabled() {
        return set < ExtraArmor.sets(owner);
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        owner.onEquipItem(slot, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return enabled() && stack.canEquip(slot, owner);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        if (!stack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        }
        // not SlotItemHandler's "only if something can be extracted": the creative screen asks this before
        // every click, so an empty slot would ignore clicks there
        return true;
    }

    @Override
    public boolean isActive() {
        if (!owner.level().isClientSide()) return true;
        return enabled() && !hiddenOnClient && shownOnClient.test(set);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        ResourceLocation icon = switch (slot) {
            case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            default -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
        };
        return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
    }

    /**
     * The menu is built in the Player constructor, before NeoForge loads the player's saved data, and
     * loading (or respawning) replaces the attachment object. So the slots must look the attachment up
     * on every access instead of keeping the object they saw first.
     */
    private record LiveInventory(Player owner) implements IItemHandlerModifiable {
        private ExtraArmorInventory inv() {
            return ExtraArmor.inventory(owner);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            inv().setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return inv().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inv().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inv().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inv().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inv().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inv().isItemValid(slot, stack);
        }
    }
}
