package net.louisbeer.client.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.loader.api.FabricLoader
import net.louisbeer.ZeroXfeclicker
import net.louisbeer.client.module.ModuleManager
import net.louisbeer.client.module.Setting
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ModConfig {
	private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
	private val configDir: Path =
		FabricLoader.getInstance().configDir.resolve("netlouisbeer")
	private val configPath: Path = configDir.resolve("config.json")

	var panelX: Int? = null
	var panelY: Int? = null

	fun load() {
		if (!Files.exists(configPath)) {
			return
		}
		try {
			val root = JsonParser.parseString(Files.readString(configPath)).asJsonObject
			val gui = root.getAsJsonObject("gui")
			if (gui != null) {
				if (gui.has("x") && !gui.get("x").isJsonNull) {
					panelX = gui.get("x").asInt
				}
				if (gui.has("y") && !gui.get("y").isJsonNull) {
					panelY = gui.get("y").asInt
				}
			}

			val modules = root.getAsJsonObject("modules") ?: return
			for (module in ModuleManager.modules) {
				val entry = modules.getAsJsonObject(module.name) ?: continue
				if (entry.has("enabled")) {
					module.setEnabled(entry.get("enabled").asBoolean, notify = false, save = false)
				}
				val settings = entry.getAsJsonObject("settings") ?: continue
				for (setting in module.settings) {
					if (!settings.has(setting.name)) {
						continue
					}
					val value = settings.get(setting.name)
					when (setting) {
						is Setting.Slider -> setting.value = value.asDouble.coerceIn(setting.min, setting.max)
						is Setting.Keybind -> {
							val name = value.asString
							setting.key = if (name.isBlank() || name == "none") {
								InputConstants.UNKNOWN
							} else {
								runCatching { InputConstants.getKey(name) }.getOrDefault(InputConstants.UNKNOWN)
							}
						}
					}
				}
			}
		} catch (t: Throwable) {
			ZeroXfeclicker.LOGGER.warn("Failed to load config from {}", configPath, t)
		}
	}

	fun save() {
		try {
			Files.createDirectories(configDir)
			val root = JsonObject()
			val gui = JsonObject()
			if (panelX != null) {
				gui.addProperty("x", panelX)
			}
			if (panelY != null) {
				gui.addProperty("y", panelY)
			}
			root.add("gui", gui)

			val modules = JsonObject()
			for (module in ModuleManager.modules) {
				val entry = JsonObject()
				entry.addProperty("enabled", module.enabled)
				val settings = JsonObject()
				for (setting in module.settings) {
					when (setting) {
						is Setting.Slider -> settings.addProperty(setting.name, setting.value)
						is Setting.Keybind -> {
							val keyName = if (setting.key == InputConstants.UNKNOWN) {
								"none"
							} else {
								setting.key.name
							}
							settings.addProperty(setting.name, keyName)
						}
					}
				}
				entry.add("settings", settings)
				modules.add(module.name, entry)
			}
			root.add("modules", modules)

			val tmp = configPath.resolveSibling("${configPath.fileName}.tmp")
			Files.writeString(tmp, gson.toJson(root))
			try {
				Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
			} catch (_: AtomicMoveNotSupportedException) {
				Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING)
			}
		} catch (t: Throwable) {
			ZeroXfeclicker.LOGGER.warn("Failed to save config to {}", configPath, t)
		}
	}

	fun setPanelPosition(x: Int, y: Int) {
		panelX = x
		panelY = y
		save()
	}
}
