package net.louisbeer.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.command.ModCommands
import net.louisbeer.client.gui.ClickGuiScreen
import net.louisbeer.client.module.ModuleManager
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object ZeroXfeclickerClient : ClientModInitializer {
	private val CATEGORY = KeyMapping.Category.register(ZeroXfeclicker.id("0xfe"))

	lateinit var openGuiKey: KeyMapping
		private set

	override fun onInitializeClient() {
		openGuiKey = KeyBindingHelper.registerKeyBinding(
			KeyMapping(
				"key.0xfeclicker.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				CATEGORY,
			),
		)

		ModCommands.register()

		ClientTickEvents.END_CLIENT_TICK.register(::onEndTick)
		ZeroXfeclicker.LOGGER.info("0xfeclicker client ready")
	}

	private fun onEndTick(client: Minecraft) {
		while (openGuiKey.consumeClick()) {
			if (client.screen is ClickGuiScreen) {
				client.setScreen(null)
			} else if (client.screen == null) {
				ClickGuiScreen.open()
			}
		}

		if (client.screen == null) {
			// Module toggle keys are handled via keyboard mixin while in-game.
		}

		ModuleManager.tick(client)
	}
}
