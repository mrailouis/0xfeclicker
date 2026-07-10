package net.louisbeer.client.gui.component

import net.louisbeer.client.gui.render.KawaseBlur
import net.louisbeer.client.module.Module
import net.louisbeer.client.module.Setting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.KeyEvent
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

	val headerHeight = 18
	val rowHeight = 16

	fun contentHeight(): Int {
		if (!expanded) {
			return headerHeight
		}
		return headerHeight + module.settings.size * rowHeight + 6
	}

	fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
		val enabledColor = if (module.enabled) {
			ARGB.color(220, 90, 200, 140)
		} else {
			ARGB.color(160, 40, 40, 45)
		}
		KawaseBlur.fillRounded(graphics, x, y, width, headerHeight, 4, enabledColor)
		graphics.drawString(
			net.minecraft.client.Minecraft.getInstance().font,
			module.name,
			x + 6,
			y + 5,
			ARGB.color(255, 240, 240, 240),
			false,
		)

		val status = if (module.enabled) "ON" else "OFF"
		val statusColor = if (module.enabled) ARGB.color(255, 120, 220, 160) else ARGB.color(200, 180, 180, 180)
		graphics.drawString(
			net.minecraft.client.Minecraft.getInstance().font,
			status,
			x + width - 6 - net.minecraft.client.Minecraft.getInstance().font.width(status),
			y + 5,
			statusColor,
			false,
		)

		if (!expanded) {
			return
		}

		var rowY = y + headerHeight + 4
		for (setting in module.settings) {
			when (setting) {
				is Setting.Slider -> renderSlider(graphics, setting, rowY, mouseX, mouseY)
				is Setting.Keybind -> renderKeybind(graphics, setting, rowY)
			}
			rowY += rowHeight
		}
	}

	private fun renderSlider(graphics: GuiGraphics, setting: Setting.Slider, rowY: Int, mouseX: Int, mouseY: Int) {
		val font = net.minecraft.client.Minecraft.getInstance().font
		val label = "${setting.name}: ${setting.value.toInt()}"
		graphics.drawString(font, label, x + 8, rowY, ARGB.color(230, 220, 220, 220), false)

		val barX = x + 8
		val barY = rowY + 11
		val barW = width - 16
		val barH = 3
		graphics.fill(barX, barY, barX + barW, barY + barH, ARGB.color(160, 30, 30, 35))

		val progress = ((setting.value - setting.min) / (setting.max - setting.min)).coerceIn(0.0, 1.0)
		val fillW = (barW * progress).toInt()
		graphics.fill(barX, barY, barX + fillW, barY + barH, ARGB.color(220, 100, 190, 150))

		val knobX = barX + fillW
		graphics.fill(knobX - 2, barY - 2, knobX + 3, barY + barH + 2, ARGB.color(255, 235, 235, 235))

		if (draggingSlider == setting) {
			updateSlider(setting, mouseX)
		}
	}

	private fun renderKeybind(graphics: GuiGraphics, setting: Setting.Keybind, rowY: Int) {
		val font = net.minecraft.client.Minecraft.getInstance().font
		val text = "${setting.name}: ${setting.displayName()}"
		val color = if (setting.listening) {
			ARGB.color(255, 255, 210, 120)
		} else {
			ARGB.color(230, 220, 220, 220)
		}
		graphics.drawString(font, text, x + 8, rowY + 3, color, false)
	}

	fun mouseClicked(event: MouseButtonEvent): Boolean {
		val mx = event.x().toInt()
		val my = event.y().toInt()
		if (mx !in x until (x + width) || my !in y until (y + contentHeight())) {
			return false
		}

		if (my in y until (y + headerHeight)) {
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

		var rowY = y + headerHeight + 4
		for (setting in module.settings) {
			if (my in rowY until (rowY + rowHeight)) {
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
			rowY += rowHeight
		}
		return false
	}

	fun mouseReleased(): Boolean {
		if (draggingSlider != null) {
			draggingSlider = null
			return true
		}
		return false
	}

	fun mouseDragged(event: MouseButtonEvent): Boolean {
		val slider = draggingSlider ?: return false
		updateSlider(slider, event.x().toInt())
		return true
	}

	fun keyPressed(event: KeyEvent): Boolean {
		for (setting in module.settings) {
			if (setting is Setting.Keybind && setting.listening) {
				return false
			}
		}
		return false
	}

	private fun updateSlider(setting: Setting.Slider, mouseX: Int) {
		val barX = x + 8
		val barW = width - 16
		val t = ((mouseX - barX).toDouble() / barW.toDouble()).coerceIn(0.0, 1.0)
		val raw = Mth.lerp(t, setting.min, setting.max)
		setting.setClamped(raw)
	}
}
