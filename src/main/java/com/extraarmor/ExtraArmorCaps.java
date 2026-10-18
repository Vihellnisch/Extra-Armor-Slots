package com.extraarmor;

import com.extraarmor.mixin.RangedAttributeAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Raised caps for players, so extra enchanted armor sets keep paying off (config "raisedCaps").
 * Up to vanilla's cap (20 points) everything works exactly like vanilla: 4% per point, up to 80%.
 * Every extra set adds one more 20-point layer that cuts what is left by another 4% per point:
 * 1 extra set -> at most 96%, 2 -> 99.2%, ... never full immunity.
 */
public final class ExtraArmorCaps {
    public static final float VANILLA_CAP = 20.0F;

    /** 1 with vanilla caps, otherwise 1 + extra sets. */
    public static int layers() {
        return ExtraArmorConfig.raisedCaps() ? 1 + ExtraArmorConfig.extraSets() : 1;
    }

    public static boolean active() {
        return layers() > 1;
    }

    /** Effective armor / protection points that still count. */
    public static float cap() {
        return VANILLA_CAP * layers();
    }

    /** Armor attribute 30 and toughness 20 per layer (for everyone; only stacked sets get near them). */
    public static void applyAttributeCaps() {
        ((RangedAttributeAccessor) Attributes.ARMOR.value()).extraarmor$setMaxValue(30.0D * layers());
        ((RangedAttributeAccessor) Attributes.ARMOR_TOUGHNESS.value()).extraarmor$setMaxValue(20.0D * layers());
    }

    /** Fraction of the damage blocked by this many (effective armor or protection) points. */
    public static float reduction(float points) {
        float left = 1.0F;
        for (int layer = 0; layer < layers(); layer++) {
            left *= 1.0F - Mth.clamp(points - VANILLA_CAP * layer, 0.0F, VANILLA_CAP) / 25.0F;
        }
        return 1.0F - left;
    }

    private ExtraArmorCaps() {}
}
