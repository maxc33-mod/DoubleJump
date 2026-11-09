package fr.barton.doublejump.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * La règle qui décide si un monstre a le droit d'apparaître à un endroit.
 */
public final class SafeZoneSpawns {
	// Monstres jamais bloqués : les boss et les neutres.
	private static final Set<EntityType<?>> EXCLUDED = Set.of(
			EntityTypes.ENDER_DRAGON,
			EntityTypes.WITHER,
			EntityTypes.ENDERMAN,
			EntityTypes.PIGLIN,
			EntityTypes.ZOMBIFIED_PIGLIN
	);

	// Vrai uniquement pendant l'exécution de la commande /summon.
	private static boolean commandSpawn = false;

	private SafeZoneSpawns() {
	}

	/** Indique si ce type d'entité est un monstre concerné par les safe zones. */
	public static boolean isTargeted(EntityType<?> type) {
		return type.getCategory() == MobCategory.MONSTER && !EXCLUDED.contains(type);
	}

	/** Indique si cette apparition doit être empêchée. */
	public static boolean isBlocked(ServerLevel level, EntityType<?> type, BlockPos pos) {
		return !commandSpawn && isTargeted(type) && SafeZoneData.get(level).isProtected(pos);
	}

	/** Exécute une apparition demandée par commande, qui ne doit pas être bloquée. */
	public static boolean runCommandSpawn(BooleanSupplier spawn) {
		commandSpawn = true;
		try {
			return spawn.getAsBoolean();
		} finally {
			commandSpawn = false; // remis à zéro même si la commande échoue
		}
	}
}
