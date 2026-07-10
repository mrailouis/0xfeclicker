package net.louisbeer.client.module

import net.louisbeer.client.config.ModConfig
import net.louisbeer.client.gui.toast.ModuleToasts
import net.minecraft.client.Minecraft

abstract class Module(
	val name: String,
	val description: String = "",
) {
	var enabled: Boolean = false
		private set

	val settings: MutableList<Setting> = mutableListOf()

	protected fun <T : Setting> add(setting: T): T {
		settings += setting
		return setting
	}

	fun toggle() {
		setEnabled(!enabled)
	}

	fun setEnabled(value: Boolean, notify: Boolean = true, save: Boolean = true) {
		if (enabled == value) {
			return
		}
		enabled = value
		if (enabled) {
			onEnable()
		} else {
			onDisable()
		}
		if (notify) {
			ModuleToasts.show(this)
		}
		if (save) {
			ModConfig.save()
		}
	}

	protected open fun onEnable() {}

	protected open fun onDisable() {}

	open fun onTick(client: Minecraft) {}
}
