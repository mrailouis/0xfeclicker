package net.louisbeer.client.render

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import org.slf4j.LoggerFactory

/**
 * Neutral warped-square water field (grayscale + alpha).
 * Modulates the blurred scene without adding its own colour.
 */
object WaterTexture {
	private val LOGGER = LoggerFactory.getLogger("xfeclicker/water")
	private const val RESOURCE_PATH = "assets/xfeclicker/textures/gui/water_pbr.png"

	private var texture: GpuTexture? = null
	private var textureView: GpuTextureView? = null

	fun view(): GpuTextureView? {
		textureView?.let { return it }

		val stream = WaterTexture::class.java.classLoader.getResourceAsStream(RESOURCE_PATH)
		if (stream == null) {
			LOGGER.error("Missing water texture: {}", RESOURCE_PATH)
			return null
		}

		return try {
			val nativeImage = stream.use { NativeImage.read(it) }
			val device = RenderSystem.getDevice()
			val created = device.createTexture(
				"xfeclicker water pbr",
				GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
				TextureFormat.RGBA8,
				nativeImage.width,
				nativeImage.height,
				1,
				1,
			)
			device.createCommandEncoder().writeToTexture(created, nativeImage)
			nativeImage.close()
			texture = created
			val view = device.createTextureView(created)
			textureView = view
			view
		} catch (t: Throwable) {
			LOGGER.error("Failed to load water texture", t)
			null
		}
	}

	fun close() {
		textureView?.close()
		textureView = null
		texture?.close()
		texture = null
	}
}
