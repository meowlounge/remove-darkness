package dev.prodbyeagle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDarknessEffect implements ClientModInitializer {
	public static final String MOD_ID = "remove-darkness-effect-remastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final DarknessEffectCleaner cleaner = new DarknessEffectCleaner();

	@Override
	public void onInitializeClient() {
		LOGGER.info("RemoveDarknessEffectOLD mod initialized!");

		KeyBinding toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.rde.toggle",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			KeyBinding.Category.GAMEPLAY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			handleToggleInput(client, toggleKeyBinding);
			cleaner.onClientTick(client);
		});
	}

	private void handleToggleInput(MinecraftClient client, KeyBinding toggleKeyBinding) {
		while (toggleKeyBinding.wasPressed()) {
			boolean enabled = cleaner.toggle();
			if (client.player != null) {
				Text feedback = Text.translatable(enabled ? "message.rde.enabled" : "message.rde.disabled");
				client.player.sendMessage(feedback, true);
			}
		}
	}

	private static final class DarknessEffectCleaner {
		private boolean enabled = true;

		boolean toggle() {
			enabled = !enabled;
			return enabled;
		}

		void onClientTick(MinecraftClient client) {
			if (!enabled) {
				return;
			}

			var server = client.getServer();
			if (server == null) {
				return; // No integrated server -> not in singleplayer.
			}

			var playerManager = server.getPlayerManager();
			if (playerManager == null) {
				return;
			}

			for (ServerPlayerEntity player : playerManager.getPlayerList()) {
				if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
					player.removeStatusEffect(StatusEffects.DARKNESS);
				}
			}
		}
	}
}
