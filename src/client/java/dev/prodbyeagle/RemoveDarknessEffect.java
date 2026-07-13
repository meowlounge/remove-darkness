package dev.prodbyeagle;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDarknessEffect implements ClientModInitializer {
	public static final String MOD_ID = "remove-darkness-effect-remastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category RDE_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("remove_darkness", "category"));


	private final DarknessEffectCleaner cleaner = new DarknessEffectCleaner();

	@Override
	public void onInitializeClient() {
		LOGGER.info("RemoveDarknessEffectOLD mod initialized!");

		KeyMapping toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.rde.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			RDE_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			handleToggleInput(client, toggleKeyBinding);
			cleaner.onClientTick(client);
		});
	}

	private void handleToggleInput(Minecraft client, KeyMapping toggleKeyBinding) {
		while (toggleKeyBinding.consumeClick()) {
			boolean enabled = cleaner.toggle();
			if (client.player != null) {
				Component feedback = Component.translatable(enabled ? "message.rde.enabled" : "message.rde.disabled");
				client.player.sendOverlayMessage(feedback);
			}
		}
	}

	private static final class DarknessEffectCleaner {
		private boolean enabled = true;

		boolean toggle() {
			enabled = !enabled;
			return enabled;
		}

		void onClientTick(Minecraft client) {
			if (!enabled) {
				return;
			}

			var server = client.getSingleplayerServer();
			if (server == null) {
				return; // No integrated server -> not in singleplayer.
			}

			var playerManager = server.getPlayerList();
			if (playerManager == null) {
				return;
			}

			for (ServerPlayer player : playerManager.getPlayers()) {
				if (player.hasEffect(MobEffects.DARKNESS)) {
					player.removeEffect(MobEffects.DARKNESS);
				}
			}
		}
	}
}
