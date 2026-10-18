package com.extraarmor.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** The Config button in the mod list opens NeoForge's built-in editor for extraarmor-common.toml. */
public final class ExtraArmorConfigScreen {
    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private ExtraArmorConfigScreen() {}
}
