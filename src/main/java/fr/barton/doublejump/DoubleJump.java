package fr.barton.doublejump;

import fr.barton.doublejump.item.ModItems;
import fr.barton.doublejump.network.GeniusBoltNetwork;
import fr.barton.doublejump.network.GliderNetwork;
import fr.barton.doublejump.network.SafeZoneNetwork;
import fr.barton.doublejump.zone.SafeZoneData;
import fr.barton.doublejump.zone.SafeZoneGuard;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleJump implements ModInitializer {
	public static final String MOD_ID = "doublejump";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer && source.is(DamageTypeTags.IS_FALL)) {
				return false; // on annule les dégâts
			}
			return true; // tous les autres dégâts s'appliquent normalement
		});

		// Les messages réseau des safe zones
		SafeZoneNetwork.register();

		// L'enchantement "Éclair de génie" : message réseau et vol des éclairs
		GeniusBoltNetwork.register();

		// Les objets du mod (le planeur)
		ModItems.register();
		GliderNetwork.register(); // qui plane ? (pour dessiner la toile chez les autres joueurs)

		// Au chargement de chaque dimension, on indique combien de safe zones ont été retrouvées.
		ServerLevelEvents.LOAD.register((server, level) -> {
			LOGGER.info("{} safe zone(s) chargée(s) pour {}",
					SafeZoneData.get(level).size(), level.dimension().identifier());
		});

		// À chaque tick, les monstres qui entrent dans une safe zone sont repoussés.
		ServerTickEvents.END_LEVEL_TICK.register(SafeZoneGuard::tick);

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
