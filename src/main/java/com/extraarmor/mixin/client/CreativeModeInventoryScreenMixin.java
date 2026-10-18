package com.extraarmor.mixin.client;

import com.extraarmor.client.ExtraArmorClient;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicking the extra column's background must not count as clicking outside (which drops the held item). */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow
    private boolean hasClickedOutside;

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void extraarmor$panelIsInside(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraArmorClient.showsPanel((CreativeModeInventoryScreen) (Object) this) && ExtraArmorClient.overPanel(mouseX, mouseY, left, top)) {
            hasClickedOutside = false;
            cir.setReturnValue(false);
        }
    }
}
