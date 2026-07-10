package net.louisbeer.client.gui.render

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import java.util.OptionalInt

/**
 * Dual Kawase-style blur: one 2x downsample pass, then one 2x upsample pass.
 */
object KawaseBlur {
	private var downTarget: TextureTarget? = null
	private var upTarget: TextureTarget? = null
	private var lastWidth = -1
	private var lastHeight = -1

	fun prepare() {
		val main = Minecraft.getInstance().mainRenderTarget
		val width = main.width
		val height = main.height
		if (width <= 0 || height <= 0) {
			return
		}

		ensureTargets(width, height)

		val down = downTarget ?: return
		val up = upTarget ?: return
		val encoder = RenderSystem.getDevice().createCommandEncoder()

		val mainColor = main.colorTexture ?: return
		val upColor = up.colorTexture ?: return
		encoder.copyTextureToTexture(mainColor, upColor, 0, 0, 0, 0, 0, width, height)

		blitLinear(up, down)
		blitLinear(down, up)
	}

	fun drawRoundedPanel(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, tint: Int) {
		val up = upTarget ?: return
		val view = up.colorTextureView ?: return
		val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
		val setup = TextureSetup.singleTexture(view, sampler)
		val guiWidth = graphics.guiWidth()
		val guiHeight = graphics.guiHeight()

		// Full-screen textured draw + scissor keeps framebuffer UVs 1:1 with the game view.
		graphics.enableScissor(x, y, x + width, y + height)
		graphics.fill(RenderPipelines.GUI_TEXTURED, setup, 0, 0, guiWidth, guiHeight)
		graphics.disableScissor()

		fillRounded(graphics, x, y, width, height, radius, tint)
		drawRoundedOutline(graphics, x, y, width, height, radius, ARGB.color(80, 255, 255, 255))
	}

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

	private fun drawRoundedOutline(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
		val r = radius.coerceIn(0, minOf(width, height) / 2)
		graphics.fill(x + r, y, x + width - r, y + 1, color)
		graphics.fill(x + r, y + height - 1, x + width - r, y + height, color)
		graphics.fill(x, y + r, x + 1, y + height - r, color)
		graphics.fill(x + width - 1, y + r, x + width, y + height - r, color)
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

	private fun blitLinear(from: RenderTarget, to: RenderTarget) {
		val fromView = from.colorTextureView ?: return
		val toView = to.colorTextureView ?: return
		val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)

		RenderSystem.getDevice()
			.createCommandEncoder()
			.createRenderPass({ "0xfe kawase blit" }, toView, OptionalInt.empty())
			.use { renderPass ->
				renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT)
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
		downTarget = TextureTarget("0xfe-kawase-down", halfW, halfH, true)
		upTarget = TextureTarget("0xfe-kawase-up", width, height, true)
		lastWidth = width
		lastHeight = height
	}
}
