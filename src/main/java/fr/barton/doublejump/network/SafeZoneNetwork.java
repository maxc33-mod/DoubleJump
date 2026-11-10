package fr.barton.doublejump.network;

import fr.barton.doublejump.DoubleJump;
import fr.barton.doublejump.zone.SafeZone;
import fr.barton.doublejump.zone.SafeZoneActions;
import fr.barton.doublejump.zone.SafeZoneData;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Les messages échangés entre le serveur et les clients à propos des safe zones.
 */
public final class SafeZoneNetwork {

	/** Serveur vers client : la liste complète des zones de la dimension du joueur. */
	public record SyncPayload(List<SafeZone> zones) implements CustomPacketPayload {
		public static final Type<SyncPayload> TYPE = new Type<>(DoubleJump.id("sync_safe_zones"));
		public static final StreamCodec<ByteBuf, SyncPayload> CODEC = SafeZone.STREAM_CODEC
				.apply(ByteBufCodecs.list())
				.map(SyncPayload::new, SyncPayload::zones);

		@Override
		public Type<SyncPayload> type() {
			return TYPE;
		}
	}

	/** Client vers serveur : associer la boussole tenue en main à cette Magnétite. */
	public record LinkCompassPayload(BlockPos pos) implements CustomPacketPayload {
		public static final Type<LinkCompassPayload> TYPE = new Type<>(DoubleJump.id("link_compass"));
		public static final StreamCodec<ByteBuf, LinkCompassPayload> CODEC = BlockPos.STREAM_CODEC
				.map(LinkCompassPayload::new, LinkCompassPayload::pos);

		@Override
		public Type<LinkCompassPayload> type() {
			return TYPE;
		}
	}

	/** Client vers serveur : agrandir cette zone, actuellement au niveau indiqué. */
	public record UpgradeZonePayload(BlockPos pos, int expectedLevel) implements CustomPacketPayload {
		public static final Type<UpgradeZonePayload> TYPE = new Type<>(DoubleJump.id("upgrade_zone"));
		public static final StreamCodec<ByteBuf, UpgradeZonePayload> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, UpgradeZonePayload::pos,
				ByteBufCodecs.VAR_INT, UpgradeZonePayload::expectedLevel,
				UpgradeZonePayload::new
		);

		@Override
		public Type<UpgradeZonePayload> type() {
			return TYPE;
		}
	}

	/** Client vers serveur : se téléporter à côté de cette Magnétite (demandé depuis la carte). */
	public record TeleportPayload(BlockPos pos) implements CustomPacketPayload {
		public static final Type<TeleportPayload> TYPE = new Type<>(DoubleJump.id("teleport"));
		public static final StreamCodec<ByteBuf, TeleportPayload> CODEC = BlockPos.STREAM_CODEC
				.map(TeleportPayload::new, TeleportPayload::pos);

		@Override
		public Type<TeleportPayload> type() {
			return TYPE;
		}
	}

	private SafeZoneNetwork() {
	}

	/** À appeler une seule fois au démarrage du mod. */
	public static void register() {
		// 1. Déclaration des messages
		PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LinkCompassPayload.TYPE, LinkCompassPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpgradeZonePayload.TYPE, UpgradeZonePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.CODEC);

		// 2. Ce que fait le serveur quand il reçoit une demande d'un joueur
		ServerPlayNetworking.registerGlobalReceiver(LinkCompassPayload.TYPE,
				(payload, context) -> SafeZoneActions.linkCompass(context.player(), payload.pos()));
		ServerPlayNetworking.registerGlobalReceiver(UpgradeZonePayload.TYPE,
				(payload, context) -> SafeZoneActions.upgrade(context.player(), payload.pos(), payload.expectedLevel()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE,
				(payload, context) -> SafeZoneActions.teleport(context.player(), payload.pos()));

		// 3. Le joueur reçoit les zones quand il arrive, change de dimension ou réapparaît
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> sendTo(listener.player));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
				(player, origin, destination) -> sendTo(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sendTo(newPlayer));
	}

	/** Envoie à un joueur les zones de la dimension où il se trouve. */
	private static void sendTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, new SyncPayload(SafeZoneData.get(player.level()).toList()));
	}

	/** Envoie les zones à jour à tous les joueurs présents dans cette dimension. */
	public static void syncLevel(ServerLevel level) {
		SyncPayload payload = new SyncPayload(SafeZoneData.get(level).toList());
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
