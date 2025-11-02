package dev.prodbyeagle.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static dev.prodbyeagle.RemoveDarknessEffect.MOD_ID;

public record TogglePayload(boolean enabled) implements CustomPayload {
	public static final Id<TogglePayload> ID = new Id<>(Identifier.of(MOD_ID, "toggle"));
	public static final PacketCodec<PacketByteBuf, TogglePayload> CODEC =
			PacketCodec.of((value, buf) -> buf.writeBoolean(value.enabled()),
					buf -> new TogglePayload(buf.readBoolean()));

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ID, CODEC);
	}

	@Override
	public Id<TogglePayload> getId() {
		return ID;
	}
}
