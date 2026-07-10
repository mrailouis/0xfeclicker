package net.louisbeer.mixin;

import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void zerofe$handleDotCommands(String message, boolean addToHistory, CallbackInfo ci) {
		ChatScreen self = (ChatScreen) (Object) this;
		String normalized = self.normalizeChatMessage(message);
		if (normalized.isEmpty() || !normalized.startsWith(".")) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (addToHistory) {
			client.gui.getChat().addRecentChat(normalized);
		}

		LocalPlayer player = client.player;
		if (player == null) {
			ci.cancel();
			return;
		}

		String command = normalized.substring(1);
		if (!ClientCommandInternals.executeCommand(command)) {
			player.connection.sendCommand(command);
		}
		ci.cancel();
	}
}
