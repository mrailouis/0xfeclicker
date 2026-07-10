package net.louisbeer.client.gui.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix3x2f

/** Textured quad with independent left/right vertex colors for a continuous horizontal gradient. */
class GradientBlitRenderState(
	private val textureSetup: TextureSetup,
	private val pose: Matrix3x2f,
	private val x0: Float,
	private val y0: Float,
	private val x1: Float,
	private val y1: Float,
	private val u0: Float,
	private val u1: Float,
	private val v0: Float,
	private val v1: Float,
	private val colorLeft: Int,
	private val colorRight: Int,
	private val scissorArea: ScreenRectangle?,
) : GuiElementRenderState {
	private val bounds: ScreenRectangle? = run {
		val rect = ScreenRectangle(
			kotlin.math.floor(x0).toInt(),
			kotlin.math.floor(y0).toInt(),
			kotlin.math.ceil(x1 - x0).toInt().coerceAtLeast(1),
			kotlin.math.ceil(y1 - y0).toInt().coerceAtLeast(1),
		).transformMaxBounds(pose)
		scissorArea?.intersection(rect) ?: rect
	}

	override fun buildVertices(vertexConsumer: VertexConsumer) {
		vertexConsumer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(colorLeft)
		vertexConsumer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(colorLeft)
		vertexConsumer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(colorRight)
		vertexConsumer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(colorRight)
	}

	override fun pipeline(): RenderPipeline = RenderPipelines.GUI_TEXTURED

	override fun textureSetup(): TextureSetup = textureSetup

	override fun scissorArea(): ScreenRectangle? = scissorArea

	override fun bounds(): ScreenRectangle? = bounds
}
