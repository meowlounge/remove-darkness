package dev.prodbyeagle;

import dev.prodbyeagle.network.TogglePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveDarknessEffect implements ModInitializer {
	public static final String MOD_ID = "rde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Set<UUID> REMOVAL_DISABLED = ConcurrentHashMap.newKeySet();

	@Override
	public void onInitialize() {
		LOGGER.info("RemoveDarknessEffect mod initialized!");
		TogglePayload.register();

		ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) ->
				context.server().execute(() -> {
					ServerPlayerEntity player = context.player();

					if (player == null) {
						LOGGER.warn("Toggle payload received without an attached player; ignoring.");
						return;
					}

					UUID uuid = player.getUuid();
					String name = player.getName().getString();

					if (payload.enabled()) {
						boolean removed = REMOVAL_DISABLED.remove(uuid);
						LOGGER.info("Darkness removal enabled for {} (uuid: {}). stateRemoved={}", name, uuid, removed);
					} else {
						boolean added = REMOVAL_DISABLED.add(uuid);
						LOGGER.info("Darkness removal disabled for {} (uuid: {}). stateAdded={}", name, uuid, added);
					}
				}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.player.getUuid();
			if (REMOVAL_DISABLED.remove(uuid)) {
				LOGGER.debug("Cleared toggle state for {} (uuid: {}) on disconnect.", handler.player.getName().getString(), uuid);
			}
		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world instanceof ServerWorld serverWorld) {
				for (ServerPlayerEntity player : serverWorld.getPlayers()) {
					UUID uuid = player.getUuid();
					String name = player.getName().getString();

					if (REMOVAL_DISABLED.contains(uuid)) {
						if (LOGGER.isDebugEnabled() && player.hasStatusEffect(StatusEffects.DARKNESS)) {
							LOGGER.debug("Skipping darkness removal for {} (uuid: {}) - player disabled it.", name, uuid);
						}
						continue;
					}

					if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
						LOGGER.debug("Removing darkness effect from {} (uuid: {}).", name, uuid);
						player.removeStatusEffect(StatusEffects.DARKNESS);
					} else if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("No darkness effect detected for {} (uuid: {}); nothing to remove.", name, uuid);
					}
				}
			}
		});
	}
}
