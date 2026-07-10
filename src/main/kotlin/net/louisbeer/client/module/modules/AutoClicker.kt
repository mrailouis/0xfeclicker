package net.louisbeer.client.module.modules

import net.louisbeer.client.module.Module
import net.louisbeer.client.module.Setting
import net.louisbeer.mixin.MinecraftAccessor
import net.minecraft.client.Minecraft

object AutoClicker : Module(
	name = "AutoClicker",
	description = "Left-clicks while attack is held",
) {
	val toggleKey = add(Setting.Keybind("Toggle Key"))
	val cps = add(Setting.Slider("CPS", value = 12.0, min = 0.0, max = 25.0, step = 1.0))

	private var lastClickMs: Long = 0L

	override fun onEnable() {
		resetClickState()
	}

	override fun onDisable() {
		resetClickState()
	}

	private fun resetClickState() {
		lastClickMs = 0L
	}

	override fun onTick(client: Minecraft) {
		if (client.screen != null || client.player == null || client.gameMode == null) {
			return
		}

		if (!client.options.keyAttack.isDown || !client.mouseHandler.isMouseGrabbed) {
			return
		}

		val clicksPerSecond = cps.value
		if (clicksPerSecond <= 0.0) {
			return
		}

		val intervalMs = (1000.0 / clicksPerSecond).toLong().coerceAtLeast(1L)
		val now = System.currentTimeMillis()
		if (now - lastClickMs < intervalMs) {
			return
		}

		lastClickMs = now
		client.missTime = 0
		(client as MinecraftAccessor).invokeStartAttack()
	}
}
