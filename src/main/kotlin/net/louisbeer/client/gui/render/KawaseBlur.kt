package net.louisbeer.client.gui.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.louisbeer.client.render.ModPipelines
import net.louisbeer.client.render.WaterTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.util.ARGB
import org.joml.Matrix3x2f
import org.slf4j.LoggerFactory
import java.util.OptionalInt

/**
 * Multi-iteration Dual Kawase blur drawn through a rounded-rect SDF panel shader (blur only, no tint).
 */
object KawaseBlur {
	private val LOGGER = LoggerFactory.getLogger("xfeclicker/kawase")

	/** Number of 2x downsample steps (and matching upsamples). Higher = stronger blur. */
	private const val ITERATIONS = 3

	private var targets: Array<TextureTarget?> = arrayOfNulls(ITERATIONS + 1)
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

			val full = targets[0] ?: return
			val mainColor = main.colorTexture ?: return
			val fullColor = full.colorTexture ?: return

			RenderSystem.getDevice()
				.createCommandEncoder()
				.copyTextureToTexture(mainColor, fullColor, 0, 0, 0, 0, 0, width, height)

			// Progressive 2x downs: full -> 1/2 -> 1/4 -> 1/8
			for (i in 0 until ITERATIONS) {
				val from = targets[i] ?: return
				val to = targets[i + 1] ?: return
				runKawasePass(from, to, ModPipelines.KAWASE_DOWN)
			}

			// Progressive 2x ups: 1/8 -> 1/4 -> 1/2 -> full
			for (i in ITERATIONS downTo 1) {
				val from = targets[i] ?: return
				val to = targets[i - 1] ?: return
				runKawasePass(from, to, ModPipelines.KAWASE_UP)
			}

			ready = true
		} catch (t: Throwable) {
			LOGGER.warn("Kawase blur prepare failed", t)
			ready = false
		}
	}

	fun drawRoundedPanel(
		graphics: GuiGraphics,
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		alpha: Int = 255,
		cornerRadius: Int = 12,
	) {
		val a = alpha.coerceIn(0, 255)
		if (a <= 0 || width <= 0 || height <= 0) {
			return
		}
		if (ready) {
			val view = targets[0]?.colorTextureView
			val water = WaterTexture.view()
			if (view != null) {
				val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
				val setup = if (water != null) {
					TextureSetup.doubleTexture(view, sampler, water, sampler)
				} else {
					TextureSetup.singleTexture(view, sampler)
				}
				// R = corner radius, G = shadow pad (pixels). Quad is expanded so the shadow fits.
				val pad = SHADOW_PAD
				val color = ARGB.color(a, cornerRadius.coerceIn(0, 255), pad.coerceIn(0, 255), 255)
				graphics.guiRenderState.submitGuiElement(
					RoundedBlurRenderState(
						setup,
						Matrix3x2f(graphics.pose()),
						x - pad,
						y - pad,
						x + width + pad,
						y + height + pad,
						color,
						graphics.scissorStack.peek(),
					),
				)
				return
			}
		}
	}

	private const val SHADOW_PAD = 16

	fun fillRounded(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
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
		if (width == lastWidth && height == lastHeight && targets[0] != null) {
			return
		}

		for (target in targets) {
			target?.destroyBuffers()
		}

		var w = width
		var h = height
		for (i in 0..ITERATIONS) {
			targets[i] = TextureTarget("xfe-kawase-$i", w.coerceAtLeast(1), h.coerceAtLeast(1), false)
			w = (w / 2).coerceAtLeast(1)
			h = (h / 2).coerceAtLeast(1)
		}

		lastWidth = width
		lastHeight = height
	}
}
