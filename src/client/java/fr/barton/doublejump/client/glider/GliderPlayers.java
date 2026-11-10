package fr.barton.doublejump.client.glider;

import fr.barton.doublejump.network.GliderNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Côté client : qui est en train de planer ? (moi, et les autres joueurs visibles)
 */
public final class GliderPlayers {
	// Numéros d'entité des autres joueurs qui planent, envoyés par le serveur
	private static final Set<Integer> OTHERS = new HashSet<>();

	private GliderPlayers() {
	}

	/** À appeler une seule fois au démarrage du client. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(GliderNetwork.PlayerGlidingPayload.TYPE, (payload, context) -> {
			if (payload.gliding()) {
				OTHERS.add(payload.entityId());
			} else {
				OTHERS.remove(payload.entityId());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> OTHERS.clear());
	}

	/** Le joueur qui porte ce numéro d'entité est-il en train de planer ? */
	public static boolean isGliding(int entityId) {
		LocalPlayer me = Minecraft.getInstance().player;
		if (me != null && me.getId() == entityId) {
			return GliderFlight.isGliding(); // pour moi, on le sait directement
		}
		return OTHERS.contains(entityId);
	}
}
