package net.louisbeer

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ZeroXfeclicker : ModInitializer {
	const val MOD_ID: String = "0xfeclicker"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("0xfeclicker initialized")
	}

	fun id(path: String): Identifier =
		Identifier.fromNamespaceAndPath(MOD_ID, path)
}
