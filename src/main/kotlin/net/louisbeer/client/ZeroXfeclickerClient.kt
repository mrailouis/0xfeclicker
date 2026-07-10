package net.louisbeer.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.command.ModCommands
import net.louisbeer.client.config.ModConfig
import net.louisbeer.client.gui.ClickGuiScreen
import net.louisbeer.client.gui.toast.ModuleToasts
import net.louisbeer.client.module.ModuleManager
import net.louisbeer.client.render.ModPipelines
import net.louisbeer.client.render.WaterTexture
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object ZeroXfeclickerClient : ClientModInitializer {
	private val CATEGORY = KeyMapping.Category.register(ZeroXfeclicker.id("oxfe"))

	lateinit var openGuiKey: KeyMapping
		private set

	override fun onInitializeClient() {
		ModPipelines.bootstrap()
		ModConfig.load()
		ModuleToasts.register()

		openGuiKey = KeyBindingHelper.registerKeyBinding(
			KeyMapping(
				"key.xfeclicker.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				CATEGORY,
			),
		)

		ModCommands.register()

		ClientTickEvents.END_CLIENT_TICK.register(::onEndTick)
		ClientLifecycleEvents.CLIENT_STOPPING.register {
			ModConfig.save()
			WaterTexture.close()
		}
		ZeroXfeclicker.LOGGER.info("0xfeclicker client ready")
	}

	private fun onEndTick(client: Minecraft) {
		while (openGuiKey.consumeClick()) {
			val screen = client.screen
			if (screen is ClickGuiScreen) {
				screen.requestClose()
			} else if (screen == null) {
				ClickGuiScreen.open()
			}
		}

		ModuleManager.tick(client)
	}
}
