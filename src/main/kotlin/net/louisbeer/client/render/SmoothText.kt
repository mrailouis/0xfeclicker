package net.louisbeer.client.render

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.gui.render.GradientBlitRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import org.joml.Matrix3x2f
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * AWT-rasterized Golos text (same approach as the reference Blaze/Larp clients).
 * Supersampled + antialiased glyphs are blitted as GUI textures so edges stay smooth
 * without touching vanilla font atlas sampling.
 */
object SmoothText {
	private const val SUPERSAMPLE = 2f
	private const val PADDING = 2
	private const val MAX_CACHE = 384
	private const val GRADIENT_STRIPS = 32

	enum class Style(val size: Float, val bold: Boolean) {
		SMALL(8.5f, false),
		BODY(11f, false),
		TITLE(13f, true),
	}

	private data class Key(val text: String, val style: Style)

	private data class Cached(
		val texture: Identifier,
		val bakedWidth: Int,
		val bakedHeight: Int,
		val drawWidth: Int,
		val drawHeight: Int,
	)

	private class SmoothDynamicTexture(label: Supplier<String>, image: NativeImage) : DynamicTexture(label, image) {
		init {
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
		}
	}

	private val fonts = ConcurrentHashMap<Style, Font>()
	private val cacheLock = Any()
	private val cache = object : LinkedHashMap<Key, Cached>(MAX_CACHE, 0.75f, true) {
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Cached>?): Boolean {
			if (size <= MAX_CACHE || eldest == null) {
				return false
			}
			Minecraft.getInstance().textureManager.release(eldest.value.texture)
			return true
		}
	}
	private var nextId = 0

	fun width(text: String, style: Style = Style.BODY): Int {
		if (text.isEmpty()) {
			return 0
		}
		return cached(Key(text, style)).drawWidth
	}

	fun height(style: Style = Style.BODY): Int = cached(Key("Ay", style)).drawHeight

	fun draw(
		graphics: GuiGraphics,
		text: String,
		x: Int,
		y: Int,
		color: Int,
		style: Style = Style.BODY,
	) {
		if (text.isEmpty() || ARGB.alpha(color) == 0) {
			return
		}
		val cached = cached(Key(text, style))
		val pose = graphics.pose()
		pose.pushMatrix()
		pose.translate(x.toFloat(), y.toFloat())
		pose.scale(1f / SUPERSAMPLE, 1f / SUPERSAMPLE)
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			cached.texture,
			0,
			0,
			0f,
			0f,
			cached.bakedWidth,
			cached.bakedHeight,
			cached.bakedWidth,
			cached.bakedHeight,
			color,
		)
		pose.popMatrix()
	}

	fun drawCentered(
		graphics: GuiGraphics,
		text: String,
		centerX: Int,
		y: Int,
		color: Int,
		style: Style = Style.BODY,
	) {
		draw(graphics, text, centerX - width(text, style) / 2, y, color, style)
	}

	/**
	 * Draws [text] with a continuous horizontal gradient that scrolls between [colorA] and [colorB].
	 */
	fun drawMovingGradient(
		graphics: GuiGraphics,
		text: String,
		x: Int,
		y: Int,
		colorA: Int,
		colorB: Int,
		style: Style = Style.BODY,
		speed: Float = 0.55f,
		alpha: Int = 255,
	) {
		if (text.isEmpty() || alpha <= 0) {
			return
		}
		val cached = cached(Key(text, style))
		val texture = Minecraft.getInstance().textureManager.getTexture(cached.texture)
		val setup = TextureSetup.singleTexture(texture.textureView, texture.sampler)
		val pose = Matrix3x2f(graphics.pose())
		pose.translate(x.toFloat(), y.toFloat())
		pose.scale(1f / SUPERSAMPLE, 1f / SUPERSAMPLE)

		val phase = (System.nanoTime() / 1_000_000_000.0 * speed).toFloat()
		val a = alpha.coerceIn(0, 255)
		val scissor = graphics.scissorStack.peek()
		val w = cached.bakedWidth.toFloat()
		val h = cached.bakedHeight.toFloat()

		for (i in 0 until GRADIENT_STRIPS) {
			val t0 = i / GRADIENT_STRIPS.toFloat()
			val t1 = (i + 1) / GRADIENT_STRIPS.toFloat()
			val x0 = w * t0
			val x1 = w * t1
			val c0 = ARGB.color(a, cycleLerp(colorA, colorB, t0 + phase) and 0xFFFFFF)
			val c1 = ARGB.color(a, cycleLerp(colorA, colorB, t1 + phase) and 0xFFFFFF)
			graphics.guiRenderState.submitGuiElement(
				GradientBlitRenderState(
					setup,
					Matrix3x2f(pose),
					x0,
					0f,
					x1,
					h,
					t0,
					t1,
					0f,
					1f,
					c0,
					c1,
					scissor,
				),
			)
		}
	}

	private fun cycleLerp(colorA: Int, colorB: Int, t: Float): Int {
		val wave = (0.5 + 0.5 * sin(t.toDouble() * PI * 2.0)).toFloat()
		return ARGB.linearLerp(wave.coerceIn(0f, 1f), ARGB.opaque(colorA), ARGB.opaque(colorB))
	}

	private fun cached(key: Key): Cached {
		synchronized(cacheLock) {
			cache[key]?.let { return it }
			val baked = bake(key)
			cache[key] = baked
			return baked
		}
	}

	private fun bake(key: Key): Cached {
		val font = fontFor(key.style)
		val measure = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
		applyHints(measure)
		measure.font = font
		val metrics = measure.fontMetrics
		val textWidth = metrics.stringWidth(key.text).coerceAtLeast(1)
		val textHeight = metrics.height.coerceAtLeast(1)
		val ascent = metrics.ascent
		measure.dispose()

		val bakedWidth = ((textWidth + PADDING * 2) * SUPERSAMPLE).roundToInt().coerceAtLeast(1)
		val bakedHeight = ((textHeight + PADDING * 2) * SUPERSAMPLE).roundToInt().coerceAtLeast(1)

		val image = BufferedImage(bakedWidth, bakedHeight, BufferedImage.TYPE_INT_ARGB)
		val g = image.createGraphics()
		applyHints(g)
		g.transform = AffineTransform.getScaleInstance(SUPERSAMPLE.toDouble(), SUPERSAMPLE.toDouble())
		g.font = font
		g.color = Color.WHITE
		g.drawString(key.text, PADDING.toFloat(), (PADDING + ascent).toFloat())
		g.dispose()

		val native = NativeImage(NativeImage.Format.RGBA, bakedWidth, bakedHeight, false)
		for (yy in 0 until bakedHeight) {
			for (xx in 0 until bakedWidth) {
				val argb = image.getRGB(xx, yy)
				val a = argb ushr 24 and 0xFF
				val r = argb ushr 16 and 0xFF
				val gch = argb ushr 8 and 0xFF
				val b = argb and 0xFF
				native.setPixelABGR(xx, yy, (a shl 24) or (b shl 16) or (gch shl 8) or r)
			}
		}

		val id = ZeroXfeclicker.id("generated/text_${nextId++}")
		Minecraft.getInstance().textureManager.register(
			id,
			SmoothDynamicTexture({ "xfe_text_$nextId" }, native),
		)
		return Cached(id, bakedWidth, bakedHeight, textWidth, textHeight)
	}

	private fun fontFor(style: Style): Font =
		fonts.computeIfAbsent(style) {
			val base = runCatching {
				val path = if (style.bold) {
					"/assets/xfeclicker/font/golos_title.ttf"
				} else {
					"/assets/xfeclicker/font/golos.ttf"
				}
				SmoothText::class.java.getResourceAsStream(path)!!.use { stream ->
					Font.createFont(Font.TRUETYPE_FONT, stream)
				}
			}.getOrElse {
				Font(Font.SANS_SERIF, Font.PLAIN, style.size.toInt())
			}
			val awtStyle = if (style.bold) Font.BOLD else Font.PLAIN
			base.deriveFont(awtStyle, style.size)
		}

	private fun applyHints(g: java.awt.Graphics2D) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
	}
}
