package net.louisbeer.client.gui

import net.louisbeer.client.gui.component.ModulePanel
import net.louisbeer.client.gui.render.KawaseBlur
import net.louisbeer.client.module.ModuleManager
import net.louisbeer.client.render.ModFonts
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

class ClickGuiScreen : Screen(ModFonts.text("0xfe")) {
	private val panelWidth = 300
	private val panelPadding = 18
	private val titleAreaHeight = 48
	private lateinit var modulePanels: List<ModulePanel>

	private val titleText: Component = ModFonts.title("0xFE x Larp")

	override fun init() {
		val panelX = (width - panelWidth) / 2
		val startY = height / 2 - 90
		var y = startY + titleAreaHeight
		modulePanels = ModuleManager.modules.map { module ->
			val panel = ModulePanel(module, panelX + panelPadding, y, panelWidth - panelPadding * 2)
			y += panel.contentHeight() + 10
			panel
		}
	}

	override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		// Keep the world visible behind the frosted panel.
	}

	override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		KawaseBlur.prepare()

		val panelHeight = estimatePanelHeight()
		val panelX = (width - panelWidth) / 2
		val panelY = height / 2 - panelHeight / 2

		KawaseBlur.drawRoundedPanel(
			guiGraphics,
			panelX,
			panelY,
			panelWidth,
			panelHeight,
			ARGB.color(140, 18, 20, 26),
		)

		val titleWidth = font.width(titleText)
		guiGraphics.drawString(
			font,
			titleText,
			panelX + (panelWidth - titleWidth) / 2,
			panelY + 16,
			ARGB.color(255, 245, 245, 245),
			false,
		)

		var y = panelY + titleAreaHeight
		for (panel in modulePanels) {
			panel.x = panelX + panelPadding
			panel.y = y
			panel.render(guiGraphics, mouseX, mouseY)
			y += panel.contentHeight() + 10
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick)
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		for (panel in modulePanels) {
			if (panel.mouseClicked(event)) {
				return true
			}
		}
		return super.mouseClicked(event, doubled)
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		var handled = false
		for (panel in modulePanels) {
			handled = panel.mouseReleased() || handled
		}
		return handled || super.mouseReleased(event)
	}

	override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
		for (panel in modulePanels) {
			if (panel.mouseDragged(event)) {
				return true
			}
		}
		return super.mouseDragged(event, dx, dy)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (ModuleManager.onKeyPressed(event)) {
			return true
		}
		return super.keyPressed(event)
	}

	override fun isPauseScreen(): Boolean = false

	private fun estimatePanelHeight(): Int {
		var height = titleAreaHeight + panelPadding
		for (panel in modulePanels) {
			height += panel.contentHeight() + 10
		}
		return height + 6
	}

	companion object {
		fun open() {
			val client = net.minecraft.client.Minecraft.getInstance()
			client.setScreen(ClickGuiScreen())
		}
	}
}
