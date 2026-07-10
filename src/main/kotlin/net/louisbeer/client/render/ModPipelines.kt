package net.louisbeer.client.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderPipeline
import net.louisbeer.ZeroXfeclicker
import net.minecraft.client.renderer.RenderPipelines

object ModPipelines {
	lateinit var KAWASE_DOWN: RenderPipeline
		private set
	lateinit var KAWASE_UP: RenderPipeline
		private set
	lateinit var ROUNDED_BLUR: RenderPipeline
		private set
	lateinit var ROUNDED_RECT: RenderPipeline
		private set

	fun bootstrap() {
		KAWASE_DOWN = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
				.withLocation(ZeroXfeclicker.id("pipeline/kawase_down"))
				.withVertexShader(ZeroXfeclicker.id("core/kawase_down"))
				.withFragmentShader(ZeroXfeclicker.id("core/kawase_down"))
				.withSampler("InSampler")
				.withoutBlend()
				.withDepthWrite(false)
				.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
				.withColorWrite(true, true)
				.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
				.build(),
		)

		KAWASE_UP = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
				.withLocation(ZeroXfeclicker.id("pipeline/kawase_up"))
				.withVertexShader(ZeroXfeclicker.id("core/kawase_up"))
				.withFragmentShader(ZeroXfeclicker.id("core/kawase_up"))
				.withSampler("InSampler")
				.withoutBlend()
				.withDepthWrite(false)
				.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
				.withColorWrite(true, true)
				.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
				.build(),
		)

		val roundedBlurBuilder = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(ZeroXfeclicker.id("pipeline/rounded_blur"))
			.withVertexShader(ZeroXfeclicker.id("core/rounded_blur"))
			.withFragmentShader(ZeroXfeclicker.id("core/rounded_blur"))
			.withSampler("Sampler1")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		(roundedBlurBuilder as FabricRenderPipeline.Builder).withUsePipelineDrawModeForGui(true)
		ROUNDED_BLUR = RenderPipelines.register(roundedBlurBuilder.build())

		val roundedRectBuilder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(ZeroXfeclicker.id("pipeline/rounded_rect"))
			.withVertexShader(ZeroXfeclicker.id("core/rounded_rect"))
			.withFragmentShader(ZeroXfeclicker.id("core/rounded_rect"))
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		(roundedRectBuilder as FabricRenderPipeline.Builder).withUsePipelineDrawModeForGui(true)
		ROUNDED_RECT = RenderPipelines.register(roundedRectBuilder.build())
	}
}
