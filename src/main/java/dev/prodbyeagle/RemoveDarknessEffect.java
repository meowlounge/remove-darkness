package dev.prodbyeagle;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDarknessEffect implements ModInitializer {
	public static final String MOD_ID = "remove-darkness-effect-remastered";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("RemoveDarknessEffect mod initialized!");

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world instanceof ServerWorld serverWorld) {
				for (ServerPlayerEntity player : serverWorld.getPlayers()) {
					// check if player has the darkness effect, if yes remove that effect. that's it. lol.
					if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
						player.removeStatusEffect(StatusEffects.DARKNESS);
					}
				}
			}
		});
	}
}