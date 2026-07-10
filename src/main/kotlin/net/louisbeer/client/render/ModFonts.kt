package net.louisbeer.client.render

import net.louisbeer.ZeroXfeclicker
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

object ModFonts {
	val ID = ZeroXfeclicker.id("rubik")
	val TITLE_ID = ZeroXfeclicker.id("rubik_title")

	val STYLE: Style = Style.EMPTY.withFont(FontDescription.Resource(ID))
	val TITLE_STYLE: Style = Style.EMPTY.withFont(FontDescription.Resource(TITLE_ID))

	fun text(string: String): MutableComponent =
		Component.literal(string).withStyle(STYLE)

	fun title(string: String): MutableComponent =
		Component.literal(string).withStyle(TITLE_STYLE)
}
