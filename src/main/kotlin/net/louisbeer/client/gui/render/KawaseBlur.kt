package net.louisbeer.client.gui.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.louisbeer.client.render.ModPipelines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import org.joml.Matrix3x2f
import org.slf4j.LoggerFactory
import java.util.OptionalInt

/**
 * Dual Kawase blur (1× downsample + 1× upsample) drawn through a rounded-rect SDF panel shader.
 */
object KawaseBlur {
	private val LOGGER = LoggerFactory.getLogger("xfeclicker/kawase")

	private var downTarget: TextureTarget? = null
	private var upTarget: TextureTarget? = null
	private var lastWidth = -1
	private var lastHeight = -1
	private var ready = false

	fun prepare() {
		ready = false
		try {
			val main = Minecraft.getInstance().mainRenderTarget
			val width = main.width
			val height = main.height
			if (width <= 0 || height <= 0) {
				return
			}

			ensureTargets(width, height)

			val down = downTarget ?: return
			val up = upTarget ?: return
			val mainColor = main.colorTexture ?: return
			val upColor = up.colorTexture ?: return

			RenderSystem.getDevice()
				.createCommandEncoder()
				.copyTextureToTexture(mainColor, upColor, 0, 0, 0, 0, 0, width, height)

			// 2x down, then 2x up
			runKawasePass(up, down, ModPipelines.KAWASE_DOWN)
			runKawasePass(down, up, ModPipelines.KAWASE_UP)
			ready = true
		} catch (t: Throwable) {
			LOGGER.warn("Kawase blur prepare failed", t)
			ready = false
		}
	}

	fun drawRoundedPanel(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, tint: Int) {
		if (ready) {
			val view = upTarget?.colorTextureView
			if (view != null) {
				val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
				val setup = TextureSetup.singleTexture(view, sampler)
				graphics.guiRenderState.submitGuiElement(
					RoundedBlurRenderState(
						setup,
						Matrix3x2f(graphics.pose()),
						x,
						y,
						x + width,
						y + height,
						tint,
						graphics.scissorStack.peek(),
					),
				)
				return
			}
		}

		// Fallback if blur targets are unavailable.
		graphics.fill(x, y, x + width, y + height, tint)
	}

	fun fillRounded(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
		// Lightweight CPU rounded rect for small module chrome (not the main blur panel).
		val r = radius.coerceIn(0, minOf(width, height) / 2)
		if (r <= 0) {
			graphics.fill(x, y, x + width, y + height, color)
			return
		}

		graphics.fill(x + r, y, x + width - r, y + height, color)
		graphics.fill(x, y + r, x + r, y + height - r, color)
		graphics.fill(x + width - r, y + r, x + width, y + height - r, color)

		fillCorner(graphics, x + r, y + r, r, color, 0)
		fillCorner(graphics, x + width - r - 1, y + r, r, color, 1)
		fillCorner(graphics, x + r, y + height - r - 1, r, color, 2)
		fillCorner(graphics, x + width - r - 1, y + height - r - 1, r, color, 3)
	}

	private fun fillCorner(graphics: GuiGraphics, cx: Int, cy: Int, radius: Int, color: Int, corner: Int) {
		for (dy in 0 until radius) {
			for (dx in 0 until radius) {
				if (dx * dx + dy * dy > radius * radius) {
					continue
				}
				val px = when (corner) {
					0, 2 -> cx - dx - 1
					else -> cx + dx
				}
				val py = when (corner) {
					0, 1 -> cy - dy - 1
					else -> cy + dy
				}
				graphics.fill(px, py, px + 1, py + 1, color)
			}
		}
	}

	private fun runKawasePass(from: RenderTarget, to: RenderTarget, pipeline: RenderPipeline) {
		val fromView = from.colorTextureView ?: return
		val toView = to.colorTextureView ?: return
		val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)

		RenderSystem.getDevice()
			.createCommandEncoder()
			.createRenderPass({ "xfe kawase" }, toView, OptionalInt.empty())
			.use { renderPass ->
				renderPass.setPipeline(pipeline)
				RenderSystem.bindDefaultUniforms(renderPass)
				renderPass.bindTexture("InSampler", fromView, sampler)
				renderPass.draw(0, 3)
			}
	}

	private fun ensureTargets(width: Int, height: Int) {
		val halfW = (width / 2).coerceAtLeast(1)
		val halfH = (height / 2).coerceAtLeast(1)
		if (width == lastWidth && height == lastHeight && downTarget != null && upTarget != null) {
			return
		}

		downTarget?.destroyBuffers()
		upTarget?.destroyBuffers()
		downTarget = TextureTarget("xfe-kawase-down", halfW, halfH, false)
		upTarget = TextureTarget("xfe-kawase-up", width, height, false)
		lastWidth = width
		lastHeight = height
	}
}
