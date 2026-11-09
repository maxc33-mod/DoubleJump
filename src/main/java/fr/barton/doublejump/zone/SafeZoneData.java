package fr.barton.doublejump.zone;

import com.mojang.serialization.Codec;
import fr.barton.doublejump.DoubleJump;
import fr.barton.doublejump.network.SafeZoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les safe zones d'une dimension, sauvegardées par Minecraft avec le monde.
 * Chaque dimension (Overworld, Nether, End) possède sa propre liste.
 */
public class SafeZoneData extends SavedData {
	// Format de sauvegarde : une liste de zones, rangée sous le nom "zones".
	private static final Codec<SafeZoneData> CODEC = SafeZone.CODEC.listOf()
			.fieldOf("zones")
			.xmap(SafeZoneData::fromList, SafeZoneData::toList)
			.codec();

	// Le dernier paramètre ne doit pas être null : Minecraft 26.3 s'en sert à chaque relecture.
	// Ce type générique ne modifie pas nos données.
	private static final SavedDataType<SafeZoneData> TYPE = new SavedDataType<>(
			DoubleJump.id("safe_zones"),
			SafeZoneData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	// Les zones, retrouvables rapidement à partir de la position de leur Magnétite.
	private final Map<BlockPos, SafeZone> zones = new HashMap<>();

	public SafeZoneData() {
	}

	private static SafeZoneData fromList(List<SafeZone> list) {
		SafeZoneData data = new SafeZoneData();
		for (SafeZone zone : list) {
			data.zones.put(zone.pos(), zone);
		}
		return data;
	}

	public List<SafeZone> toList() {
		return new ArrayList<>(zones.values());
	}

	/** Récupère les zones de cette dimension (les charge depuis le disque si besoin). */
	public static SafeZoneData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public int size() {
		return zones.size();
	}

	public Collection<SafeZone> zones() {
		return zones.values();
	}

	/** Renvoie la zone de cette Magnétite, ou null si elle n'en a pas. */
	public SafeZone find(BlockPos pos) {
		return zones.get(pos);
	}

	/** Indique si ce bloc est protégé par au moins une zone. */
	public boolean isProtected(BlockPos pos) {
		for (SafeZone zone : zones.values()) {
			if (zone.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	/** Crée une zone de niveau 1 : appelé quand un joueur pose une Magnétite. */
	public static void createAt(ServerLevel level, BlockPos pos) {
		SafeZoneData data = get(level);
		SafeZone zone = new SafeZone(pos.immutable(), SafeZone.MIN_LEVEL);
		data.zones.put(zone.pos(), zone);
		data.setDirty(); // signale à Minecraft qu'il y a du nouveau à sauvegarder
		SafeZoneGuard.clearInside(level, zone);
		SafeZoneNetwork.syncLevel(level);
		DoubleJump.LOGGER.info("Safe zone créée en {} ({}), côté {} blocs",
				zone.pos().toShortString(), level.dimension().identifier(), zone.side());
	}

	/** Remplace une zone par sa version agrandie. */
	public static void upgradeAt(ServerLevel level, SafeZone zone) {
		SafeZoneData data = get(level);
		SafeZone upgraded = zone.nextLevel();
		data.zones.put(upgraded.pos(), upgraded);
		data.setDirty();
		SafeZoneGuard.clearInside(level, upgraded);
		SafeZoneNetwork.syncLevel(level);
		DoubleJump.LOGGER.info("Safe zone agrandie en {} ({}) : niveau {}, côté {} blocs",
				upgraded.pos().toShortString(), level.dimension().identifier(), upgraded.level(), upgraded.side());
	}

	/** Retire la zone éventuelle : appelé quand une Magnétite disparaît. */
	public static void removeAt(ServerLevel level, BlockPos pos) {
		SafeZoneData data = get(level);
		if (data.zones.remove(pos) != null) {
			data.setDirty();
			SafeZoneNetwork.syncLevel(level);
			DoubleJump.LOGGER.info("Safe zone supprimée en {} ({})",
					pos.toShortString(), level.dimension().identifier());
		}
	}
}
