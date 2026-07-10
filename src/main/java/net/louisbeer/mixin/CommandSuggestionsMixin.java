package net.louisbeer.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Treats '.' the same as '/' for command suggestions so {@code .0xfe} autocompletes.
 * Length is preserved ({@code .} → {@code /}), so suggestion ranges stay aligned.
 */
@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {
	@Redirect(
		method = "updateCommandInfo",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;getValue()Ljava/lang/String;")
	)
	private String zerofe$dotAsSlash(EditBox editBox) {
		String value = editBox.getValue();
		if (value.startsWith(".") && !value.startsWith("./")) {
			return "/" + value.substring(1);
		}
		return value;
	}
}
