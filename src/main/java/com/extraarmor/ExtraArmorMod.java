package com.extraarmor;

import com.extraarmor.network.ExtraArmorSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Extra Armor Slots: a second armor column next to the vanilla one. Whatever is worn there adds its
 * armor, attributes and enchantments on top of the normal armor, with raised armor/protection caps.
 * How many extra sets (columns) and whether caps are raised is set in {@link ExtraArmorConfig}.
 */
@Mod(ExtraArmorMod.MODID)
public final class ExtraArmorMod {
    public static final String MODID = "extraarmor";

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** All extra armor sets of a player. Copied on death; emptied first unless keepInventory is on. */
    public static final Supplier<AttachmentType<ExtraArmorInventory>> EXTRA_ARMOR = ATTACHMENTS.register("extra_armor",
            () -> AttachmentType.serializable(ExtraArmorInventory::new).copyOnDeath().build());

    public ExtraArmorMod(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, ExtraArmorConfig.SPEC);
        modBus.addListener(ExtraArmorConfig::onLoad);
        modBus.addListener(ExtraArmorConfig::onReload);
        if (FMLEnvironment.dist.isClient()) com.extraarmor.client.ExtraArmorConfigScreen.register(container);
        modBus.addListener(ExtraArmorSyncPayload::register);
    }
}
