package net.louisbeer.client.command

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.louisbeer.client.gui.ClickGuiScreen
import net.minecraft.network.chat.Component

object ModCommands {
	fun register() {
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			val openGui = Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> { context ->
				context.source.getClient().execute {
					ClickGuiScreen.open()
				}
				context.source.sendFeedback(Component.literal("Opened 0xfe ClickGUI"))
				Command.SINGLE_SUCCESS
			}

			dispatcher.register(
				ClientCommandManager.literal("0xfe").executes(openGui),
			)
		}
	}
}
