package net.louisbeer.client.gui.toast

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.animation.Easing
import net.louisbeer.client.gui.render.KawaseBlur
import net.louisbeer.client.module.Module
import net.louisbeer.client.render.SmoothText
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.ARGB
import kotlin.math.max

object ModuleToasts {
	private val TEXT = SmoothText.Style.SMALL
	private const val MIN_WIDTH = 108
	private const val PAD = 8
	private const val LINE_GAP = 2
	private const val CORNER_RADIUS = 8
	private const val SCREEN_MARGIN = 10
	private const val TOAST_GAP = 5
	private const val ENTER_SECONDS = 0.28f
	private const val HOLD_SECONDS = 1.85f
	private const val EXIT_SECONDS = 0.28f

	private val toasts = ArrayList<Toast>()

	fun register() {
		HudElementRegistry.addLast(ZeroXfeclicker.id("module_toasts"), ::render)
	}

	fun show(module: Module) {
		val label = if (module.enabled) "Enabled" else "Disabled"
		toasts += Toast(
			title = module.name,
			subtitle = label,
			enabled = module.enabled,
		)
		if (toasts.size > 5) {
			toasts.removeAt(0)
		}
	}

	private fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
		if (toasts.isEmpty()) {
			return
		}

		val client = Minecraft.getInstance()
		val deltaSeconds = (deltaTracker.realtimeDeltaTicks / 20.0f).coerceIn(0.0f, 0.1f)
		val screenW = client.window.guiScaledWidth
		val screenH = client.window.guiScaledHeight
		val lineHeight = SmoothText.height(TEXT)
		val toastHeight = PAD + lineHeight + LINE_GAP + lineHeight + PAD

		KawaseBlur.prepare()

		val iterator = toasts.iterator()
		var index = 0
		while (iterator.hasNext()) {
			val toast = iterator.next()
			toast.age += deltaSeconds
			val (slide, opacity) = toast.motion()
			if (toast.age >= ENTER_SECONDS + HOLD_SECONDS + EXIT_SECONDS) {
				iterator.remove()
				continue
			}

			val contentWidth = max(SmoothText.width(toast.title, TEXT), SmoothText.width(toast.subtitle, TEXT))
			val toastWidth = max(MIN_WIDTH, contentWidth + PAD * 2)
			val x = screenW - SCREEN_MARGIN - toastWidth + ((1.0f - slide) * (toastWidth + SCREEN_MARGIN + 8)).toInt()
			val y = screenH - SCREEN_MARGIN - toastHeight - index * (toastHeight + TOAST_GAP)

			val alpha = (opacity * 255.0f).toInt().coerceIn(0, 255)
			KawaseBlur.drawRoundedPanel(graphics, x, y, toastWidth, toastHeight, alpha, CORNER_RADIUS)

			val statusColor = if (toast.enabled) {
				ARGB.color(alpha, 40, 230, 110)
			} else {
				ARGB.color(alpha, 240, 70, 70)
			}

			val titleY = y + PAD
			val statusY = titleY + lineHeight + LINE_GAP
			SmoothText.draw(graphics, toast.title, x + PAD, titleY, ARGB.color(alpha, 240, 240, 245), TEXT)
			SmoothText.draw(graphics, toast.subtitle, x + PAD, statusY, statusColor, TEXT)
			index++
		}
	}

	private data class Toast(
		val title: String,
		val subtitle: String,
		val enabled: Boolean,
		var age: Float = 0.0f,
	) {
		fun motion(): Pair<Float, Float> {
			return when {
				age < ENTER_SECONDS -> {
					val t = (age / ENTER_SECONDS).coerceIn(0.0f, 1.0f)
					val eased = Easing.cubicEaseOut(t)
					eased to eased
				}
				age < ENTER_SECONDS + HOLD_SECONDS -> 1.0f to 1.0f
				else -> {
					val t = ((age - ENTER_SECONDS - HOLD_SECONDS) / EXIT_SECONDS).coerceIn(0.0f, 1.0f)
					val eased = Easing.cubicEaseIn(t)
					(1.0f - eased) to (1.0f - eased)
				}
			}
		}
	}
}
