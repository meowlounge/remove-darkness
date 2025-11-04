package dev.prodbyeagle;

import dev.prodbyeagle.network.TogglePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveDarknessEffectClient implements ClientModInitializer {
	public static final Identifier CATEGORY_CUSTOM_TITLE = Identifier.of(RemoveDarknessEffect.MOD_ID, "title");
	private static final String TOGGLE_TRANSLATION_KEY = "key." + RemoveDarknessEffect.MOD_ID + ".toggle";
	private static final String MESSAGE_PREFIX = "message." + RemoveDarknessEffect.MOD_ID + ".";
	private static final String WARNING_TRANSLATION_KEY = MESSAGE_PREFIX + "server_warning";
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
			new KeyBinding(
					TOGGLE_TRANSLATION_KEY,
					InputUtil.Type.KEYSYM,
					GLFW.GLFW_KEY_G,
					KeyBinding.Category.create(CATEGORY_CUSTOM_TITLE)
			)
	);
	private static final Logger LOGGER = RemoveDarknessEffect.LOGGER;
	private static final Set<String> WARNED_SERVERS = ConcurrentHashMap.newKeySet();
	private static boolean removalEnabled = true;
	private static boolean serverSupportsToggle = true;
	private static boolean loggedUnsupportedWarning = false;
	private static boolean notifiedToggleUnsupported = false;

	private static boolean sendTogglePacket(boolean enabled) {
		boolean canSend = ClientPlayNetworking.canSend(TogglePayload.ID);

		if (!canSend) {
			serverSupportsToggle = false;
			if (!loggedUnsupportedWarning) {
				LOGGER.warn("Skipping toggle packet; server missing {}", TogglePayload.ID.id());
				loggedUnsupportedWarning = true;
			} else {
				LOGGER.debug("Skipping toggle packet; server still missing {}", TogglePayload.ID.id());
			}
			return false;
		}

		serverSupportsToggle = true;
		loggedUnsupportedWarning = false;
		notifiedToggleUnsupported = false;
		LOGGER.debug("Sending toggle packet with enabled={} (previous state={}).", enabled, removalEnabled);
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
		if (client.player != null && !notifiedToggleUnsupported) {
			client.player.sendMessage(Text.translatable(MESSAGE_PREFIX + "unsupported"), true);
			client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.1F, 0.4F);
			LOGGER.debug("Server reported unsupported payload; notified client player.");
			notifiedToggleUnsupported = true;
		}
	}

	private static void showFirstJoinWarning(ClientPlayNetworkHandler handler, MinecraftClient client) {
		if (client.isIntegratedServerRunning()) {
			return;
		}

		String serverKey = resolveServerIdentifier(handler, client);
		if (!WARNED_SERVERS.add(serverKey)) {
			return;
		}

		client.execute(() -> {
			if (ClientPlayNetworking.canSend(TogglePayload.ID)) {
				return;
			}

			LOGGER.warn("Remote server detected ({}); advising player to disable the mod.", serverKey);
			if (client.player != null) {
				client.player.sendMessage(Text.translatable(WARNING_TRANSLATION_KEY), false);
			}
		});
	}

	private static String resolveServerIdentifier(ClientPlayNetworkHandler handler, MinecraftClient client) {
		if (client.getCurrentServerEntry() != null) {
			return client.getCurrentServerEntry().address;
		}

		SocketAddress address = handler.getConnection().getAddress();
		return address != null ? address.toString() : "unknown";
	}

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			removalEnabled = true;
			serverSupportsToggle = ClientPlayNetworking.canSend(TogglePayload.ID);
			loggedUnsupportedWarning = false;
			notifiedToggleUnsupported = false;
			showFirstJoinWarning(handler, client);
			LOGGER.info("Joined server; reset removal flag.");
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			removalEnabled = true;
			serverSupportsToggle = true;
			loggedUnsupportedWarning = false;
			notifiedToggleUnsupported = false;
			LOGGER.debug("Client disconnected; reset removal flag.");
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			boolean canSendNow = ClientPlayNetworking.canSend(TogglePayload.ID);
			if (canSendNow && !serverSupportsToggle) {
				LOGGER.info("Server now accepts toggle payloads; re-enabling toggle key.");
				serverSupportsToggle = true;
				loggedUnsupportedWarning = false;
				notifiedToggleUnsupported = false;
			} else if (!canSendNow && serverSupportsToggle) {
				serverSupportsToggle = false;
			}

			while (TOGGLE_KEY.wasPressed()) {
				if (!serverSupportsToggle) {
					LOGGER.debug("Toggle ignored; server does not support {}", TogglePayload.ID.id());
					sendUnsupportedFeedback(client);
					continue;
				}

				boolean nextState = !removalEnabled;
				LOGGER.debug("Toggle key pressed; attempting to set removalEnabled={} (previous state={}).", nextState, removalEnabled);

				if (sendTogglePacket(nextState)) {
					removalEnabled = nextState;
					LOGGER.info("Darkness removal toggled {}; awaiting server confirmation.", removalEnabled ? "on" : "off");
					sendFeedback(client, removalEnabled);
				} else {
					if (serverSupportsToggle) {
						LOGGER.warn("Failed to send toggle packet for state {}.", nextState);
					}
					sendUnsupportedFeedback(client);
				}
			}
		});
	}
}
