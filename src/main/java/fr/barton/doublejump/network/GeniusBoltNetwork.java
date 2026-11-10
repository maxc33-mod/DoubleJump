package fr.barton.doublejump.network;

import fr.barton.doublejump.DoubleJump;
import fr.barton.doublejump.genius.GeniusBolts;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le message envoyé par le client à chaque coup d'épée chargé, et la mise en route des éclairs.
 */
public final class GeniusBoltNetwork {

	/**
	 * Client vers serveur : lancer un éclair de génie.
	 * ignoredEntityId est le mob frappé au corps à corps par ce même coup (-1 si aucun) :
	 * l'éclair le traverse, pour ne pas le blesser deux fois.
	 */
	public record LaunchPayload(int ignoredEntityId) implements CustomPacketPayload {
		public static final Type<LaunchPayload> TYPE = new Type<>(DoubleJump.id("launch_genius_bolt"));
		public static final StreamCodec<ByteBuf, LaunchPayload> CODEC = ByteBufCodecs.INT
				.map(LaunchPayload::new, LaunchPayload::ignoredEntityId);

		@Override
		public Type<LaunchPayload> type() {
			return TYPE;
		}
	}

	private GeniusBoltNetwork() {
	}

	/** À appeler une seule fois au démarrage du mod. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(LaunchPayload.TYPE, LaunchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LaunchPayload.TYPE,
				(payload, context) -> GeniusBolts.launch(context.player(), payload.ignoredEntityId()));

		// Les éclairs avancent à chaque tick du serveur
		ServerTickEvents.END_SERVER_TICK.register(GeniusBolts::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> GeniusBolts.forget(listener.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> GeniusBolts.clear());
	}
}
