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

		ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
			boolean enabled = payload.enabled();
			ServerPlayerEntity player = context.player();

			if (enabled) {
				REMOVAL_DISABLED.remove(player.getUuid());
			} else {
				REMOVAL_DISABLED.add(player.getUuid());
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				REMOVAL_DISABLED.remove(handler.player.getUuid()));

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world instanceof ServerWorld serverWorld) {
				for (ServerPlayerEntity player : serverWorld.getPlayers()) {
					if (!REMOVAL_DISABLED.contains(player.getUuid())
							&& player.hasStatusEffect(StatusEffects.DARKNESS)) {
						player.removeStatusEffect(StatusEffects.DARKNESS);
					}
				}
			}
		});
	}
}
