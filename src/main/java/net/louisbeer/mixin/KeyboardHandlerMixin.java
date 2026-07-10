package net.louisbeer.mixin;

import net.louisbeer.client.module.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void zerofe$onKeyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
		if (action != 1) {
			return;
		}
		if (this.minecraft.screen != null) {
			return;
		}
		if (ModuleManager.INSTANCE.onKeyPressed(keyEvent)) {
			ci.cancel();
		}
	}
}
