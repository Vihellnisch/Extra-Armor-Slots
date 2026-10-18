package com.extraarmor.network;

import com.extraarmor.ExtraArmor;
import com.extraarmor.ExtraArmorInventory;
import com.extraarmor.ExtraArmorMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: a player's extra armor stacks, so everyone sees them on that player, plus how many
 * sets the server allows (the client shows that many columns).
 */
public record ExtraArmorSyncPayload(int entityId, int sets, List<ItemStack> stacks) implements CustomPacketPayload {
    public static final Type<ExtraArmorSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExtraArmorMod.MODID, "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraArmorSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExtraArmorSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, ExtraArmorSyncPayload::sets,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, ExtraArmorSyncPayload::stacks,
            ExtraArmorSyncPayload::new);

    public static ExtraArmorSyncPayload of(Player player) {
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) stacks.add(inv.getStackInSlot(i).copy());
        return new ExtraArmorSyncPayload(player.getId(), ExtraArmor.sets(player), stacks);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, STREAM_CODEC, ExtraArmorSyncPayload::handle);
    }

    private static void handle(ExtraArmorSyncPayload payload, IPayloadContext context) {
        ExtraArmor.clientSets = Math.max(1, Math.min(ExtraArmor.MAX_SETS, payload.sets()));
        if (!(context.player().level().getEntity(payload.entityId()) instanceof Player player)) return;
        ExtraArmorInventory inv = ExtraArmor.inventory(player);
        for (int i = 0; i < Math.min(inv.getSlots(), payload.stacks().size()); i++) {
            inv.setStackInSlot(i, payload.stacks().get(i));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
