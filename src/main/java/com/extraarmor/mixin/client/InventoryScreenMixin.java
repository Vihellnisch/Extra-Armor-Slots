package com.extraarmor.mixin.client;

import com.extraarmor.client.ExtraArmorClient;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicking the extra column's background must not count as clicking outside (which drops the held item). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void extraarmor$panelIsInside(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraArmorClient.showsPanel((InventoryScreen) (Object) this) && ExtraArmorClient.overPanel(mouseX, mouseY, left, top)) {
            cir.setReturnValue(false);
        }
    }
}
