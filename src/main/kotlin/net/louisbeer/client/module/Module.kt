package net.louisbeer.client.module

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

	fun setEnabled(value: Boolean) {
		if (enabled == value) {
			return
		}
		enabled = value
		if (enabled) {
			onEnable()
		} else {
			onDisable()
		}
	}

	protected open fun onEnable() {}

	protected open fun onDisable() {}

	open fun onTick(client: Minecraft) {}
}
