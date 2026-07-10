package net.louisbeer.client.gui.component

import net.louisbeer.client.config.ModConfig
import net.louisbeer.client.gui.render.GuiShapes
import net.louisbeer.client.module.Module
import net.louisbeer.client.module.Setting
import net.louisbeer.client.render.SmoothText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.ARGB
import net.minecraft.util.Mth

class ModulePanel(
	private val module: Module,
	var x: Int,
	var y: Int,
	val width: Int,
) {
	private var expanded = true
	private var draggingSlider: Setting.Slider? = null

	fun contentHeight(): Int {
		if (!expanded) {
			return HEADER_HEIGHT
		}
		var height = HEADER_HEIGHT + SETTINGS_TOP_PAD
		for (setting in module.settings) {
			height += rowHeight(setting)
		}
		return height + BOTTOM_PAD
	}

	fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, opacity: Float = 1.0f) {
		val alpha = (opacity * 255.0f).toInt().coerceIn(0, 255)
		val textH = SmoothText.height()
		val headerTextY = y + (HEADER_HEIGHT - textH) / 2

		SmoothText.draw(
			graphics,
			module.name,
			x + CONTENT_PAD,
			headerTextY,
			ARGB.color(alpha, 240, 240, 240),
		)

		val status = if (module.enabled) "ON" else "OFF"
		val statusColor = if (module.enabled) {
			ARGB.color(alpha, 40, 230, 110)
		} else {
			ARGB.color(alpha, 240, 70, 70)
		}
		SmoothText.draw(
			graphics,
			status,
			x + width - CONTENT_PAD - SmoothText.width(status),
			headerTextY,
			statusColor,
		)

		if (!expanded) {
			return
		}

		var rowY = y + HEADER_HEIGHT + SETTINGS_TOP_PAD
		for (setting in module.settings) {
			when (setting) {
				is Setting.Slider -> renderSlider(graphics, setting, rowY, mouseX, alpha)
				is Setting.Keybind -> renderKeybind(graphics, setting, rowY, alpha)
			}
			rowY += rowHeight(setting)
		}
	}

	private fun renderSlider(
		graphics: GuiGraphics,
		setting: Setting.Slider,
		rowY: Int,
		mouseX: Int,
		alpha: Int,
	) {
		val label = "${setting.name}: ${setting.value.toInt()}"
		SmoothText.draw(
			graphics,
			label,
			x + CONTENT_PAD,
			rowY,
			ARGB.color((alpha * 0.9f).toInt().coerceIn(0, 255), 220, 220, 220),
		)

		val barX = x + CONTENT_PAD
		val barY = rowY + SmoothText.height() + LABEL_BAR_GAP
		val barW = width - CONTENT_PAD * 2
		val barH = BAR_HEIGHT
		GuiShapes.capsule(graphics, barX, barY, barW, barH, ARGB.color((alpha * 0.65f).toInt().coerceIn(0, 255), 30, 30, 35))

		val progress = ((setting.value - setting.min) / (setting.max - setting.min)).coerceIn(0.0, 1.0)
		val fillW = (barW * progress).toInt()
		if (fillW > 0) {
			GuiShapes.capsule(
				graphics,
				barX,
				barY,
				fillW.coerceAtLeast(barH),
				barH,
				ARGB.color((alpha * 0.9f).toInt().coerceIn(0, 255), 100, 190, 150),
			)
		}

		val knobSize = 10
		val knobX = (barX + fillW - knobSize / 2).coerceIn(barX, barX + barW - knobSize)
		val knobY = barY + barH / 2 - knobSize / 2
		GuiShapes.capsule(graphics, knobX, knobY, knobSize, knobSize, ARGB.color(alpha, 240, 240, 245))

		if (draggingSlider == setting) {
			updateSlider(setting, mouseX)
		}
	}

	private fun renderKeybind(graphics: GuiGraphics, setting: Setting.Keybind, rowY: Int, alpha: Int) {
		val text = "${setting.name}: ${setting.displayName()}"
		val color = if (setting.listening) {
			ARGB.color(alpha, 255, 210, 120)
		} else {
			ARGB.color((alpha * 0.9f).toInt().coerceIn(0, 255), 220, 220, 220)
		}
		val textY = rowY + (KEYBIND_ROW_HEIGHT - SmoothText.height()) / 2
		SmoothText.draw(graphics, text, x + CONTENT_PAD, textY, color)
	}

	fun mouseClicked(event: MouseButtonEvent): Boolean {
		val mx = event.x().toInt()
		val my = event.y().toInt()
		if (mx !in x until (x + width) || my !in y until (y + contentHeight())) {
			return false
		}

		if (my in y until (y + HEADER_HEIGHT)) {
			if (event.button() == 0) {
				module.toggle()
				return true
			}
			if (event.button() == 1) {
				expanded = !expanded
				return true
			}
		}

		if (!expanded || event.button() != 0) {
			return false
		}

		var rowY = y + HEADER_HEIGHT + SETTINGS_TOP_PAD
		for (setting in module.settings) {
			val height = rowHeight(setting)
			if (my in rowY until (rowY + height)) {
				when (setting) {
					is Setting.Slider -> {
						draggingSlider = setting
						updateSlider(setting, mx)
						return true
					}
					is Setting.Keybind -> {
						setting.listening = !setting.listening
						return true
					}
				}
			}
			rowY += height
		}
		return false
	}

	fun mouseReleased(): Boolean {
		if (draggingSlider != null) {
			draggingSlider = null
			ModConfig.save()
			return true
		}
		return false
	}

	fun cancelDrag() {
		draggingSlider = null
	}

	fun mouseDragged(event: MouseButtonEvent): Boolean {
		val slider = draggingSlider ?: return false
		updateSlider(slider, event.x().toInt())
		return true
	}

	private fun updateSlider(setting: Setting.Slider, mouseX: Int) {
		val barX = x + CONTENT_PAD
		val barW = width - CONTENT_PAD * 2
		val t = ((mouseX - barX).toDouble() / barW.toDouble()).coerceIn(0.0, 1.0)
		val raw = Mth.lerp(t, setting.min, setting.max)
		setting.setClamped(raw)
	}

	private fun rowHeight(setting: Setting): Int = when (setting) {
		is Setting.Slider -> SLIDER_ROW_HEIGHT
		is Setting.Keybind -> KEYBIND_ROW_HEIGHT
	}

	companion object {
		const val HEADER_HEIGHT = 26
		const val CONTENT_PAD = 10
		const val SETTINGS_TOP_PAD = 6
		const val BOTTOM_PAD = 10
		const val LABEL_BAR_GAP = 5
		const val BAR_HEIGHT = 6
		const val SLIDER_ROW_HEIGHT = 34
		const val KEYBIND_ROW_HEIGHT = 22

		fun estimateHeight(module: Module, expanded: Boolean = true): Int {
			if (!expanded) {
				return HEADER_HEIGHT
			}
			var height = HEADER_HEIGHT + SETTINGS_TOP_PAD + BOTTOM_PAD
			for (setting in module.settings) {
				height += when (setting) {
					is Setting.Slider -> SLIDER_ROW_HEIGHT
					is Setting.Keybind -> KEYBIND_ROW_HEIGHT
				}
			}
			return height
		}
	}
}
