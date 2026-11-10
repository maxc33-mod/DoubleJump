package fr.barton.doublejump.client.genius;

import fr.barton.doublejump.genius.GeniusEnchantment;
import fr.barton.doublejump.network.GeniusBoltNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Côté client : à chaque coup d'épée chargé, on demande au serveur de lancer un éclair.
 */
public final class GeniusBoltInput {
	// Comme pour les coups critiques du jeu : la barre d'attaque doit être remplie à plus de 90 %
	private static final float MIN_CHARGE = 0.9F;

	private GeniusBoltInput() {
	}

	/** À appeler une seule fois au démarrage du client. */
	public static void register() {
		ClientPreAttackCallback.EVENT.register(GeniusBoltInput::onAttack);
	}

	// Appelé quand le bouton d'attaque (clic gauche) est enfoncé, avant que le jeu ne traite le coup
	private static boolean onAttack(Minecraft client, LocalPlayer player, int clickCount) {
		if (clickCount > 0 && canLaunch(client, player)) {
			// Si le coup touche aussi un mob de près, l'éclair l'ignorera
			int ignoredId = -1;
			if (client.hitResult instanceof EntityHitResult entityHit) {
				ignoredId = entityHit.getEntity().getId();
			}
			ClientPlayNetworking.send(new GeniusBoltNetwork.LaunchPayload(ignoredId));
		}
		return false; // on laisse toujours le jeu faire son coup d'épée normal
	}

	private static boolean canLaunch(Minecraft client, LocalPlayer player) {
		return client.missTime <= 0                              // pas juste après un coup raté
				&& !player.isSpectator()
				&& !player.isUsingItem()                          // pas en train de manger, bloquer...
				&& !isAimingAtBlock(client)                       // un clic sur un bloc sert à creuser
				&& player.getAttackStrengthScale(0.5F) > MIN_CHARGE
				&& GeniusEnchantment.isOn(player.level(), player.getMainHandItem());
	}

	private static boolean isAimingAtBlock(Minecraft client) {
		return client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK;
	}
}
