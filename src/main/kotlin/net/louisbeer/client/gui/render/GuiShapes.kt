package net.louisbeer.client.gui.render

import net.minecraft.client.gui.GuiGraphics
import org.joml.Matrix3x2f

object GuiShapes {
	fun roundedRect(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Int) {
		if (width <= 0 || height <= 0) {
			return
		}
		graphics.guiRenderState.submitGuiElement(
			RoundedRectRenderState(
				Matrix3x2f(graphics.pose()),
				x,
				y,
				x + width,
				y + height,
				color,
				graphics.scissorStack.peek(),
			),
		)
	}

	fun capsule(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Int) {
		roundedRect(graphics, x, y, width.coerceAtLeast(height), height, color)
	}
}
