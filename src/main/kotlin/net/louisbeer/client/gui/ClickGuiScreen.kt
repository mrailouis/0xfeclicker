package net.louisbeer.client.gui

import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.gui.component.ModulePanel
import net.louisbeer.client.gui.render.KawaseBlur
import net.louisbeer.client.module.ModuleManager
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB

class ClickGuiScreen : Screen(Component.literal("0xfe")) {
	private val panelWidth = 220
	private val panelPadding = 14
	private val cornerRadius = 12
	private lateinit var modulePanels: List<ModulePanel>

	private val titleText: Component = Component.literal("0xFE x Larp")
		.withStyle(Style.EMPTY.withFont(FontDescription.Resource(ZeroXfeclicker.id("rubik"))))

	override fun init() {
		val panelX = (width - panelWidth) / 2
		val startY = height / 2 - 70
		var y = startY + 36
		modulePanels = ModuleManager.modules.map { module ->
			val panel = ModulePanel(module, panelX + panelPadding, y, panelWidth - panelPadding * 2)
			y += panel.contentHeight() + 8
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
			cornerRadius,
			ARGB.color(120, 12, 14, 18),
		)

		val titleWidth = font.width(titleText)
		guiGraphics.drawString(
			font,
			titleText,
			panelX + (panelWidth - titleWidth) / 2,
			panelY + 12,
			ARGB.color(255, 245, 245, 245),
			false,
		)

		var y = panelY + 36
		for (panel in modulePanels) {
			panel.x = panelX + panelPadding
			panel.y = y
			panel.render(guiGraphics, mouseX, mouseY)
			y += panel.contentHeight() + 8
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
		var height = 36 + panelPadding
		for (panel in modulePanels) {
			height += panel.contentHeight() + 8
		}
		return height + 4
	}

	companion object {
		fun open() {
			val client = net.minecraft.client.Minecraft.getInstance()
			client.setScreen(ClickGuiScreen())
		}
	}
}
