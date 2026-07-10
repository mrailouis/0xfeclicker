package net.louisbeer

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ZeroXfeclicker : ModInitializer {
	/** Fabric mod IDs cannot start with a digit, so we use xfeclicker. */
	const val MOD_ID: String = "xfeclicker"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("0xfeclicker initialized")
	}

	fun id(path: String): Identifier =
		Identifier.fromNamespaceAndPath(MOD_ID, path)
}
