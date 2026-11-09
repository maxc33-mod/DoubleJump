package fr.barton.doublejump.client.zone;

import fr.barton.doublejump.zone.SafeZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Le halo bleu très clair, visible sur les faces d'une safe zone quand le joueur en est proche.
 */
public final class SafeZoneHalo {
	// Distance maximale entre le joueur et les particules du halo.
	private static final double RANGE = 10.0;
	// Nombre d'essais par face et par tick : augmenter cette valeur rend le halo plus dense.
	private static final int ATTEMPTS_PER_FACE = 12;
	// Couleur (rouge, vert, bleu en hexadécimal) et taille des particules.
	private static final DustParticleOptions PARTICLE = new DustParticleOptions(0xBFE8FF, 1.0f);

	private SafeZoneHalo() {
	}

	/** À appeler à chaque tick du client. */
	public static void tick(Minecraft client) {
		if (client.player == null || client.level == null || client.isPaused()) {
			return;
		}

		Vec3 eye = client.player.getEyePosition();
		for (SafeZone zone : ClientSafeZones.zones()) {
			if (isNear(zone, eye)) {
				for (Direction side : Direction.values()) {
					drawFace(client.level, zone, side, eye);
				}
			}
		}
	}

	// Évite tout calcul pour les zones trop éloignées du joueur.
	private static boolean isNear(SafeZone zone, Vec3 eye) {
		double limit = zone.radius() + 0.5 + RANGE;
		for (Direction.Axis axis : Direction.Axis.values()) {
			if (Math.abs(eye.get(axis) - center(zone, axis)) > limit) {
				return false;
			}
		}
		return true;
	}

	private static void drawFace(ClientLevel level, SafeZone zone, Direction side, Vec3 eye) {
		double distance = zone.distanceToFace(eye, side);
		if (Math.abs(distance) > RANGE) {
			return; // le joueur est trop loin de cette face
		}

		Direction.Axis faceAxis = side.getAxis();
		double face = eye.get(faceAxis) + side.getAxisDirection().getStep() * distance;
		ThreadLocalRandom random = ThreadLocalRandom.current();

		for (int attempt = 0; attempt < ATTEMPTS_PER_FACE; attempt++) {
			// On tire un point autour du joueur, placé exactement sur le plan de la face.
			double[] coordinates = new double[3];
			for (Direction.Axis axis : Direction.Axis.values()) {
				coordinates[axis.ordinal()] = axis == faceAxis
						? face
						: eye.get(axis) + random.nextDouble(-RANGE, RANGE);
			}
			Vec3 point = new Vec3(coordinates[0], coordinates[1], coordinates[2]);

			if (point.distanceTo(eye) <= RANGE && isOnFace(zone, point, faceAxis) && !isInOtherZone(zone, point)) {
				level.addParticle(PARTICLE, point.x, point.y, point.z, 0.0, 0.0, 0.0);
			}
		}
	}

	// Le point tiré au hasard reste-t-il dans les limites de la face ?
	private static boolean isOnFace(SafeZone zone, Vec3 point, Direction.Axis faceAxis) {
		double limit = zone.radius() + 0.5;
		for (Direction.Axis axis : Direction.Axis.values()) {
			if (axis != faceAxis && Math.abs(point.get(axis) - center(zone, axis)) > limit) {
				return false;
			}
		}
		return true;
	}

	// Pas de halo là où une autre zone prend le relais : seule la limite extérieure est visible.
	private static boolean isInOtherZone(SafeZone zone, Vec3 point) {
		BlockPos block = BlockPos.containing(point);
		for (SafeZone other : ClientSafeZones.zones()) {
			if (!other.pos().equals(zone.pos()) && other.contains(block)) {
				return true;
			}
		}
		return false;
	}

	// Coordonnée du centre de la Magnétite sur cet axe.
	private static double center(SafeZone zone, Direction.Axis axis) {
		return zone.pos().get(axis) + 0.5;
	}
}
