package com.extraarmor;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/extraarmor-common.toml (also editable in-game: Mods > Extra Armor Slots > Config).
 * The server's values count; clients get the number of sets from the server.
 */
public final class ExtraArmorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue EXTRA_SETS = BUILDER
            .comment("How many extra armor sets a player can wear (extra columns next to the normal armor).",
                    "Lowering it gives the armor in removed columns back to the player.")
            .defineInRange("extraArmorSets", 1, 1, ExtraArmor.MAX_SETS);

    public static final ModConfigSpec.BooleanValue RAISED_CAPS = BUILDER
            .comment("true: every extra set raises the armor / toughness / protection caps by one vanilla cap,",
                    "so stacked sets keep making you tougher (never fully immune).",
                    "false: vanilla caps (armor 30, toughness 20, at most 80% blocked by armor and 80% by protection).")
            .define("raisedCaps", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int extraSets() {
        return SPEC.isLoaded() ? EXTRA_SETS.get() : EXTRA_SETS.getDefault();
    }

    public static boolean raisedCaps() {
        return SPEC.isLoaded() ? RAISED_CAPS.get() : RAISED_CAPS.getDefault();
    }

    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) ExtraArmorCaps.applyAttributeCaps();
    }

    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) ExtraArmorCaps.applyAttributeCaps();
    }

    private ExtraArmorConfig() {}
}
