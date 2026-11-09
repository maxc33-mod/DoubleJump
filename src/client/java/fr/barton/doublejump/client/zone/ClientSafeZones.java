package fr.barton.doublejump.client.zone;

import fr.barton.doublejump.zone.SafeZone;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La copie, côté client, des safe zones de la dimension où se trouve le joueur.
 * Elle est remplacée à chaque message reçu du serveur.
 */
public final class ClientSafeZones {
	private static final Map<BlockPos, SafeZone> ZONES = new HashMap<>();

	private ClientSafeZones() {
	}

	public static void replaceAll(List<SafeZone> zones) {
		ZONES.clear();
		for (SafeZone zone : zones) {
			ZONES.put(zone.pos(), zone);
		}
	}

	public static void clear() {
		ZONES.clear();
	}

	public static Collection<SafeZone> zones() {
		return ZONES.values();
	}

	/** Renvoie la zone de cette Magnétite, ou null si elle n'en a pas. */
	public static SafeZone get(BlockPos pos) {
		return ZONES.get(pos);
	}
}
