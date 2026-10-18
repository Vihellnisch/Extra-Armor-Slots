package com.extraarmor.mixin;

import com.extraarmor.ExtraArmor;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The creative inventory edits slots client-side and reports them with set-creative-slot packets,
 * which the server only accepts up to slot 45 (the offhand). Let the extra slots (46 onwards) through.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
    private int extraarmor$acceptExtraSlots(int lastSlot) {
        return lastSlot + ExtraArmor.COUNT;
    }
}
