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

import static dev.prodbyeagle.RemoveDarknessEffect.MOD_ID;

public class RemoveDarknessEffectClient implements ClientModInitializer {
	public static final Identifier CATEGORY_CUSTOM_TITLE = Identifier.of(MOD_ID, "title");

	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
			new KeyBinding(
					"key.rde.toggle",
					InputUtil.Type.KEYSYM,
					GLFW.GLFW_KEY_G,
					KeyBinding.Category.create(CATEGORY_CUSTOM_TITLE)
			)
	);
	private static boolean removalEnabled = true;

	private static void sendTogglePacket(boolean enabled) {
		ClientPlayNetworking.send(new TogglePayload(enabled));
	}

	private static void sendFeedback(MinecraftClient client, boolean enabled) {
		if (client.player != null) {
			client.player.sendMessage(
					Text.translatable(
							enabled
									? "message.rde.enabled"
									: "message.rde.disabled"
					),
					true
			);
			client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.1F, 2F);
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
				removalEnabled = !removalEnabled;
				sendTogglePacket(removalEnabled);
				sendFeedback(client, removalEnabled);
			}
		});
	}
}
