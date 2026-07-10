package net.louisbeer.client.module

import com.mojang.blaze3d.platform.InputConstants
import net.louisbeer.client.config.ModConfig
import net.louisbeer.client.module.modules.AutoClicker
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent

object ModuleManager {
	val modules: List<Module> = listOf(
		AutoClicker,
	)

	fun tick(client: Minecraft) {
		for (module in modules) {
			if (module.enabled) {
				module.onTick(client)
			}
		}
	}

	fun onKeyPressed(keyEvent: KeyEvent): Boolean {
		val key = InputConstants.getKey(keyEvent) // KEYSYM or SCANCODE
		for (module in modules) {
			for (setting in module.settings) {
				if (setting is Setting.Keybind) {
					if (setting.listening) {
						if (keyEvent.isEscape) {
							setting.key = InputConstants.UNKNOWN
						} else {
							setting.key = key
						}
						setting.listening = false
						ModConfig.save()
						return true
					}

					if (setting.key != InputConstants.UNKNOWN && setting.key == key) {
						module.toggle()
						return true
					}
				}
			}
		}
		return false
	}
}
