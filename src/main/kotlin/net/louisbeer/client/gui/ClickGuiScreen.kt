package net.louisbeer.client.gui

import net.louisbeer.client.animation.GuiAnimation
import net.louisbeer.client.config.ModConfig
import net.louisbeer.client.gui.component.ModulePanel
import net.louisbeer.client.gui.render.KawaseBlur
import net.louisbeer.client.module.ModuleManager
import net.louisbeer.client.render.ModFonts
import net.louisbeer.client.render.SmoothText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.ARGB

class ClickGuiScreen : Screen(ModFonts.text("0xfe")) {
	private val panelWidth = 300
	private val panelPadding = 16
	private val titleAreaHeight = 44
	private val moduleGap = 8
	private lateinit var modulePanels: List<ModulePanel>

	private val animation = GuiAnimation.opening()
	private var forceClose = false
	private var closeWhenSafe = false
	private var dragging = false
	private var dragOffsetX = 0
	private var dragOffsetY = 0
	private var panelX = 0
	private var panelY = 0

	override fun init() {
		val defaultX = (width - panelWidth) / 2
		val defaultY = height / 2 - estimatePanelHeight() / 2
		panelX = ModConfig.panelX?.coerceIn(0, (width - panelWidth).coerceAtLeast(0)) ?: defaultX
		panelY = ModConfig.panelY?.coerceIn(0, (height - 40).coerceAtLeast(0)) ?: defaultY

		rebuildModulePanels()
	}

	private fun rebuildModulePanels() {
		var y = panelY + titleAreaHeight
		modulePanels = ModuleManager.modules.map { module ->
			val panel = ModulePanel(module, panelX + panelPadding, y, panelWidth - panelPadding * 2)
			y += panel.contentHeight() + moduleGap
			panel
		}
	}

	override fun tick() {
		if (closeWhenSafe) {
			forceClose = true
			closeWhenSafe = false
			minecraft.setScreen(null)
		}
	}

	override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		// Keep the world visible behind the frosted panel.
	}

	override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		val client = minecraft
		val deltaSeconds = client.deltaTracker.realtimeDeltaTicks / 20.0f
		animation.tick(deltaSeconds.coerceIn(0.0f, 0.1f))

		// Never call setScreen from render — it breaks mouse grab. Defer to tick().
		if (animation.hasFinishedClosing) {
			closeWhenSafe = true
			return
		}

		val opacity = animation.opacity
		val alpha = (opacity * 255.0f).toInt().coerceIn(0, 255)
		if (alpha <= 0) {
			return
		}

		KawaseBlur.prepare()

		val panelHeight = estimatePanelHeight()
		val scale = animation.scale
		val cx = panelX + panelWidth / 2.0f
		val cy = panelY + panelHeight / 2.0f

		val pose = guiGraphics.pose()
		pose.pushMatrix()
		pose.translate(cx, cy + animation.verticalOffset)
		pose.scale(scale, scale)
		pose.translate(-cx, -cy)

		KawaseBlur.drawRoundedPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, alpha, cornerRadius = 12)

		val titleStyle = SmoothText.Style.TITLE
		val brand = "0xFE"
		val mid = " x "
		val larp = "Larp"
		val titleY = panelY + (titleAreaHeight - SmoothText.height(titleStyle)) / 2
		val titleWidth =
			SmoothText.width(brand, titleStyle) + SmoothText.width(mid, titleStyle) + SmoothText.width(larp, titleStyle)
		val titleX = panelX + (panelWidth - titleWidth) / 2
		SmoothText.drawMovingGradient(
			guiGraphics,
			brand,
			titleX,
			titleY,
			ARGB.color(255, 255, 92, 168),
			ARGB.color(255, 52, 211, 120),
			titleStyle,
			speed = 0.45f,
			alpha = alpha,
		)
		val midX = titleX + SmoothText.width(brand, titleStyle)
		SmoothText.draw(
			guiGraphics,
			mid,
			midX,
			titleY,
			ARGB.color(alpha, 240, 240, 245),
			titleStyle,
		)
		SmoothText.drawMovingGradient(
			guiGraphics,
			larp,
			midX + SmoothText.width(mid, titleStyle),
			titleY,
			ARGB.color(255, 30, 58, 138),
			ARGB.color(255, 255, 92, 168),
			titleStyle,
			speed = 0.45f,
			alpha = alpha,
		)

		var y = panelY + titleAreaHeight
		for (panel in modulePanels) {
			panel.x = panelX + panelPadding
			panel.y = y
			panel.render(guiGraphics, mouseX, mouseY, opacity)
			y += panel.contentHeight() + moduleGap
		}

		pose.popMatrix()
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (animation.isClosing || closeWhenSafe) {
			return true
		}

		val mx = event.x().toInt()
		val my = event.y().toInt()
		val panelHeight = estimatePanelHeight()

		if (event.button() == 0 && mx in panelX until (panelX + panelWidth) && my in panelY until (panelY + titleAreaHeight)) {
			dragging = true
			dragOffsetX = mx - panelX
			dragOffsetY = my - panelY
			return true
		}

		if (mx in panelX until (panelX + panelWidth) && my in panelY until (panelY + panelHeight)) {
			for (panel in modulePanels) {
				if (panel.mouseClicked(event)) {
					return true
				}
			}
			return true
		}

		return super.mouseClicked(event, doubled)
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		var handled = false
		if (dragging) {
			dragging = false
			ModConfig.setPanelPosition(panelX, panelY)
			handled = true
		}
		for (panel in modulePanels) {
			handled = panel.mouseReleased() || handled
		}
		return handled || super.mouseReleased(event)
	}

	override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
		if (dragging) {
			val panelHeight = estimatePanelHeight()
			panelX = (event.x().toInt() - dragOffsetX).coerceIn(0, (width - panelWidth).coerceAtLeast(0))
			panelY = (event.y().toInt() - dragOffsetY).coerceIn(0, (height - panelHeight).coerceAtLeast(0))
			rebuildModulePanels()
			return true
		}
		for (panel in modulePanels) {
			if (panel.mouseDragged(event)) {
				return true
			}
		}
		return super.mouseDragged(event, dx, dy)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (animation.isClosing || closeWhenSafe) {
			return true
		}
		if (ModuleManager.onKeyPressed(event)) {
			return true
		}
		return super.keyPressed(event)
	}

	override fun onClose() {
		if (forceClose) {
			super.onClose()
			return
		}
		requestClose()
	}

	fun requestClose() {
		if (animation.isClosing || closeWhenSafe) {
			return
		}
		dragging = false
		for (panel in modulePanels) {
			panel.cancelDrag()
		}
		animation.startClosing()
	}

	override fun isPauseScreen(): Boolean = false

	private fun estimatePanelHeight(): Int {
		if (::modulePanels.isInitialized) {
			var height = titleAreaHeight + panelPadding
			for (panel in modulePanels) {
				height += panel.contentHeight() + moduleGap
			}
			return height
		}
		var height = titleAreaHeight + panelPadding
		for (module in ModuleManager.modules) {
			height += ModulePanel.estimateHeight(module) + moduleGap
		}
		return height
	}

	companion object {
		fun open() {
			val client = net.minecraft.client.Minecraft.getInstance()
			client.setScreen(ClickGuiScreen())
		}
	}
}
