package com.extraarmor.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Slot positions are final in vanilla; the extra columns move between pages, so the client repositions them. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void extraarmor$setX(int x);

    @Mutable
    @Accessor("y")
    void extraarmor$setY(int y);
}
