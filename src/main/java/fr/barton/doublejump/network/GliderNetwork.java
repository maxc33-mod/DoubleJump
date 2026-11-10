package fr.barton.doublejump.network;

import fr.barton.doublejump.DoubleJump;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Les messages qui permettent à chacun de savoir qui est en train de planer,
 * pour dessiner la toile au-dessus des autres joueurs.
 * Le vol lui-même est calculé par le client du joueur qui plane.
 */
public final class GliderNetwork {

	/** Client vers serveur : "je commence à planer" (true) ou "j'arrête" (false). */
	public record GlidingPayload(boolean gliding) implements CustomPacketPayload {
		public static final Type<GlidingPayload> TYPE = new Type<>(DoubleJump.id("gliding"));
		public static final StreamCodec<ByteBuf, GlidingPayload> CODEC = ByteBufCodecs.BOOL
				.map(GlidingPayload::new, GlidingPayload::gliding);

		@Override
		public Type<GlidingPayload> type() {
			return TYPE;
		}
	}

	/** Serveur vers client : tel joueur (repéré par son numéro d'entité) plane, ou ne plane plus. */
	public record PlayerGlidingPayload(int entityId, boolean gliding) implements CustomPacketPayload {
		public static final Type<PlayerGlidingPayload> TYPE = new Type<>(DoubleJump.id("player_gliding"));
		public static final StreamCodec<ByteBuf, PlayerGlidingPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, PlayerGlidingPayload::entityId,
				ByteBufCodecs.BOOL, PlayerGlidingPayload::gliding,
				PlayerGlidingPayload::new
		);

		@Override
		public Type<PlayerGlidingPayload> type() {
			return TYPE;
		}
	}

	// Les joueurs qui planent en ce moment
	private static final Set<UUID> GLIDING = new HashSet<>();

	private GliderNetwork() {
	}

	/** À appeler une seule fois au démarrage du mod. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(GlidingPayload.TYPE, GlidingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PlayerGlidingPayload.TYPE, PlayerGlidingPayload.CODEC);

		// Un joueur annonce qu'il commence ou arrête de planer : on prévient ceux qui le voient
		ServerPlayNetworking.registerGlobalReceiver(GlidingPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (payload.gliding()) {
				GLIDING.add(player.getUUID());
			} else {
				GLIDING.remove(player.getUUID());
			}
			PlayerGlidingPayload message = new PlayerGlidingPayload(player.getId(), payload.gliding());
			for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
				ServerPlayNetworking.send(viewer, message);
			}
		});

		// Un joueur arrive à portée de vue d'un autre : on lui dit si cet autre plane déjà
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof ServerPlayer glider && GLIDING.contains(glider.getUUID())) {
				ServerPlayNetworking.send(viewer, new PlayerGlidingPayload(glider.getId(), true));
			}
		});
		// Il sort de sa vue : on efface l'information
		EntityTrackingEvents.STOP_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof ServerPlayer glider) {
				ServerPlayNetworking.send(viewer, new PlayerGlidingPayload(glider.getId(), false));
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register(
				(listener, server) -> GLIDING.remove(listener.player.getUUID()));
	}
}
