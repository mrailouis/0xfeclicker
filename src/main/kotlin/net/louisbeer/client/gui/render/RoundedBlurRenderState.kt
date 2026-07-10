package net.louisbeer.client.gui.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.louisbeer.client.render.ModPipelines
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import org.joml.Matrix3x2f

/**
 * Textured panel quad with local UVs (0..1) for the rounded-rect SDF shader.
 * The fragment shader samples the Kawase target via gl_FragCoord.
 */
class RoundedBlurRenderState(
	private val textureSetup: TextureSetup,
	private val pose: Matrix3x2f,
	private val x0: Int,
	private val y0: Int,
	private val x1: Int,
	private val y1: Int,
	private val color: Int,
	private val scissorArea: ScreenRectangle?,
) : GuiElementRenderState {
	private val bounds: ScreenRectangle? =
		getBounds(x0, y0, x1, y1, pose, scissorArea)

	override fun buildVertices(vertexConsumer: VertexConsumer) {
		vertexConsumer.addVertexWith2DPose(pose, x0.toFloat(), y0.toFloat()).setUv(0f, 0f).setColor(color)
		vertexConsumer.addVertexWith2DPose(pose, x0.toFloat(), y1.toFloat()).setUv(0f, 1f).setColor(color)
		vertexConsumer.addVertexWith2DPose(pose, x1.toFloat(), y1.toFloat()).setUv(1f, 1f).setColor(color)
		vertexConsumer.addVertexWith2DPose(pose, x1.toFloat(), y0.toFloat()).setUv(1f, 0f).setColor(color)
	}

	override fun pipeline(): RenderPipeline = ModPipelines.ROUNDED_BLUR

	override fun textureSetup(): TextureSetup = textureSetup

	override fun scissorArea(): ScreenRectangle? = scissorArea

	override fun bounds(): ScreenRectangle? = bounds

	companion object {
		private fun getBounds(
			x0: Int,
			y0: Int,
			x1: Int,
			y1: Int,
			pose: Matrix3x2f,
			scissor: ScreenRectangle?,
		): ScreenRectangle? {
			val rect = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
			return scissor?.intersection(rect) ?: rect
		}
	}
}
