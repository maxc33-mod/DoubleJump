package fr.barton.doublejump.genius;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tous les éclairs en vol, et les lancers demandés par les joueurs.
 * Le serveur revérifie tout : on ne fait jamais confiance à ce qu'envoie un client.
 */
public final class GeniusBolts {
	// Les éclairs en train de voler
	private static final List<GeniusBolt> ACTIVE = new ArrayList<>();

	// Pour chaque joueur, le moment (en ticks) de son dernier lancer
	private static final Map<UUID, Long> LAST_LAUNCH = new HashMap<>();

	private GeniusBolts() {
	}

	/**
	 * Un joueur demande à lancer un éclair (il vient de donner un coup d'épée chargé).
	 * ignoredEntityId : le mob frappé au corps à corps par ce même coup (-1 si aucun).
	 */
	public static void launch(ServerPlayer player, int ignoredEntityId) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}

		ItemStack sword = player.getMainHandItem();
		if (!GeniusEnchantment.isOn(player.level(), sword)) {
			return;
		}

		// Barre d'attaque : on refuse les lancers trop rapprochés (anti-triche).
		// Une marge de 2 ticks évite de refuser un coup légitime à cause du réseau.
		long now = player.level().getGameTime();
		Long last = LAST_LAUNCH.get(player.getUUID());
		double minimumGap = player.getCurrentItemAttackStrengthDelay() * 0.9 - 2;
		if (last != null && now - last < minimumGap) {
			return;
		}
		LAST_LAUNCH.put(player.getUUID(), now);

		// Son du trident "Canalisation", plus aigu et moins fort que l'original
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.6F);

		GeniusBolt bolt = new GeniusBolt(player, sword, ignoredEntityId);
		// Premier pas tout de suite, pour que l'éclair parte sans attendre
		if (!bolt.tick()) {
			ACTIVE.add(bolt);
		}
	}

	/** À chaque tick du serveur, les éclairs avancent ; ceux qui ont fini disparaissent. */
	public static void tick(MinecraftServer server) {
		Iterator<GeniusBolt> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick()) {
				iterator.remove();
			}
		}
	}

	/** Quand un joueur se déconnecte, on oublie son dernier lancer. */
	public static void forget(ServerPlayer player) {
		LAST_LAUNCH.remove(player.getUUID());
	}

	/** À l'arrêt du serveur (ou en quittant un monde solo), on vide tout. */
	public static void clear() {
		ACTIVE.clear();
		LAST_LAUNCH.clear();
	}
}
