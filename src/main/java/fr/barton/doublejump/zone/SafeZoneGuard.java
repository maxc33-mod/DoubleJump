package fr.barton.doublejump.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Applique les safe zones aux monstres : fait disparaître ceux qui s'y trouvent
 * quand une zone apparaît ou s'agrandit, et repousse ceux qui essaient d'y entrer.
 */
public final class SafeZoneGuard {
	// Épaisseur de la bande, juste à l'intérieur de la limite, où un monstre est considéré comme "entrant".
	private static final double SHELL = 2.0;
	// Distance à laquelle le monstre est replacé à l'extérieur de la limite.
	private static final double MARGIN = 0.5;
	// Petit élan donné vers l'extérieur.
	private static final double PUSH_SPEED = 0.3;

	private SafeZoneGuard() {
	}

	/** À appeler à chaque tick de chaque dimension. */
	public static void tick(ServerLevel level) {
		SafeZoneData data = SafeZoneData.get(level);
		if (data.size() == 0) {
			return;
		}
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof Mob mob && SafeZoneSpawns.isTargeted(mob.getType())) {
				repel(level, data, mob);
			}
		}
	}

	/** Fait disparaître, sans butin ni expérience, les monstres non nommés présents dans la zone. */
	public static void clearInside(ServerLevel level, SafeZone zone) {
		// On note d'abord les monstres : on ne retire rien pendant le parcours de la liste du jeu.
		List<Mob> toRemove = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof Mob mob && SafeZoneSpawns.isTargeted(mob.getType())
					&& !mob.hasCustomName() && zone.contains(mob.blockPosition())) {
				toRemove.add(mob);
			}
		}
		toRemove.forEach(Entity::discard);
	}

	private static void repel(ServerLevel level, SafeZoneData data, Mob mob) {
		BlockPos at = mob.blockPosition();
		for (SafeZone zone : data.zones()) {
			if (zone.contains(at) && pushOut(level, data, zone, mob)) {
				return;
			}
		}
	}

	// Essaie de replacer le monstre juste derrière la face qu'il vient de franchir.
	private static boolean pushOut(ServerLevel level, SafeZoneData data, SafeZone zone, Mob mob) {
		// Un monstre sur une monture bouge avec elle : c'est donc la monture qu'on déplace.
		Entity moved = mob.getRootVehicle();
		Vec3 position = moved.position();

		for (Direction side : Direction.values()) {
			double distance = zone.distanceToFace(position, side);
			if (distance > SHELL) {
				continue; // le monstre n'est pas près de cette face
			}

			Vec3 outward = side.getUnitVec3();
			Vec3 offset = outward.scale(distance + MARGIN);
			if (data.isProtected(BlockPos.containing(position.add(offset)))) {
				continue; // cette face donne sur une autre zone : ce n'est pas la limite extérieure
			}

			// On essaie au même niveau, puis un bloc plus haut si le terrain monte.
			for (int lift = 0; lift <= 1; lift++) {
				Vec3 move = offset.add(0, lift, 0);
				if (level.noCollision(moved, moved.getBoundingBox().move(move))) {
					moved.setPos(position.add(move));
					moved.setDeltaMovement(outward.scale(PUSH_SPEED));
					mob.getNavigation().stop();
					return true;
				}
			}
		}
		return false;
	}
}
