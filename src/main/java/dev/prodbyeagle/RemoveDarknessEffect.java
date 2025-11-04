package dev.prodbyeagle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDarknessEffect implements ClientModInitializer {
	public static final String MOD_ID = "remove-darkness-effect-remastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final DarknessEffectCleaner cleaner = new DarknessEffectCleaner();

	@Override
	public void onInitializeClient() {
		LOGGER.info("RemoveDarknessEffect mod initialized!");
		ServerTickEvents.END_SERVER_TICK.register(cleaner::onEndServerTick);
	}

	private static final class DarknessEffectCleaner {
		void onEndServerTick(MinecraftServer server) {
			if (!server.isSingleplayer()) {
				return;
			}

			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
					player.removeStatusEffect(StatusEffects.DARKNESS);
				}
			}
		}
	}
}
