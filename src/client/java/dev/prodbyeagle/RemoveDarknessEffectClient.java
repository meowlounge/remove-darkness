package dev.prodbyeagle;

import dev.prodbyeagle.network.TogglePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class RemoveDarknessEffectClient implements ClientModInitializer {
	private static final String TOGGLE_TRANSLATION_KEY = "key." + RemoveDarknessEffect.MOD_ID + ".toggle";
	private static final String MESSAGE_PREFIX = "message." + RemoveDarknessEffect.MOD_ID + ".";

	public static final Identifier CATEGORY_CUSTOM_TITLE = Identifier.of(RemoveDarknessEffect.MOD_ID, "title");

	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
			new KeyBinding(
					TOGGLE_TRANSLATION_KEY,
					InputUtil.Type.KEYSYM,
					GLFW.GLFW_KEY_G,
					KeyBinding.Category.create(CATEGORY_CUSTOM_TITLE)
			)
	);
	private static boolean removalEnabled = true;

	private static boolean sendTogglePacket(boolean enabled) {
		if (!ClientPlayNetworking.canSend(TogglePayload.ID)) {
			RemoveDarknessEffect.LOGGER.warn("Skipping toggle packet; server missing {}", TogglePayload.ID.id());
			return false;
		}

		ClientPlayNetworking.send(new TogglePayload(enabled));
		return true;
	}

	private static void sendFeedback(MinecraftClient client, boolean enabled) {
		if (client.player != null) {
			client.player.sendMessage(Text.translatable(MESSAGE_PREFIX + (enabled ? "enabled" : "disabled")), true);
			client.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 0.4F, 0.3F);
		}
	}

	private static void sendUnsupportedFeedback(MinecraftClient client) {
		if (client.player != null) {
			client.player.sendMessage(Text.translatable(MESSAGE_PREFIX + "unsupported"), true);
			client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.1F, 0.4F);
		}
	}

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			removalEnabled = true;
			sendTogglePacket(true);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> removalEnabled = true);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			while (TOGGLE_KEY.wasPressed()) {
				boolean nextState = !removalEnabled;

				if (sendTogglePacket(nextState)) {
					removalEnabled = nextState;
					sendFeedback(client, removalEnabled);
				} else {
					sendUnsupportedFeedback(client);
				}
			}
		});
	}
}
