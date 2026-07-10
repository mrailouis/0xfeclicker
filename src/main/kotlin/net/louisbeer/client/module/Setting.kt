package net.louisbeer.client.module

import com.mojang.blaze3d.platform.InputConstants

sealed class Setting(val name: String) {
	class Slider(
		name: String,
		var value: Double,
		val min: Double,
		val max: Double,
		val step: Double = 1.0,
	) : Setting(name) {
		fun setClamped(raw: Double) {
			val stepped = (Math.round(raw / step) * step).coerceIn(min, max)
			value = stepped
		}
	}

	class Keybind(
		name: String,
		var key: InputConstants.Key = InputConstants.UNKNOWN,
		var listening: Boolean = false,
	) : Setting(name) {
		fun displayName(): String {
			if (listening) {
				return "..."
			}
			return if (key == InputConstants.UNKNOWN) {
				"None"
			} else {
				key.displayName.string
			}
		}
	}
}
