package fr.barton.doublejump.client.glider;

import fr.barton.doublejump.item.ModItems;
import fr.barton.doublejump.network.GliderNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Le vol en planeur, calculé côté client (comme le double saut : en Minecraft,
 * c'est le client qui décide des mouvements de son joueur).
 */
public final class GliderFlight {
	// Vitesses, en blocs par tick (20 ticks = 1 seconde)
	private static final double FORWARD_SPEED = 0.4;  // 8 blocs/s, un peu plus qu'un sprint
	private static final double SIDE_SPEED = 0.15;    // glissade à gauche / à droite (Q / D)
	// Descente normale : 1 bloc/s. Elle doit rester supérieure à 0.03125 bloc/tick,
	// sinon un serveur multijoueur croit que le joueur vole et peut l'expulser.
	private static final double SINK_SPEED = 0.05;
	private static final double DIVE_SPEED = 0.25;    // descente en piqué (S) : 5 blocs/s

	private static boolean gliding = false;
	private static boolean wasHolding = false;
	private static GliderSound sound = null;

	private GliderFlight() {
	}

	/** Vrai pendant un vol en planeur. */
	public static boolean isGliding() {
		return gliding;
	}

	/** Le joueur tient-il le planeur dans la main (barre d'action) ? */
	public static boolean isHoldingGlider(LocalPlayer player) {
		return player.getMainHandItem().is(ModItems.GLIDER);
	}

	/** Appelé par le double saut : avec le planeur en main, le vol commence. */
	public static void onDoubleJump(LocalPlayer player) {
		if (isHoldingGlider(player)) {
			start();
		}
	}

	/** À appeler à chaque tick du client. */
	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			gliding = false; // pas de connexion : rien à prévenir
			wasHolding = false;
			return;
		}

		boolean holding = isHoldingGlider(player);

		// Planeur sélectionné pendant une chute : le vol se lance tout seul
		if (!gliding && holding && !wasHolding && !mustStop(player)
				&& player.getDeltaMovement().y < 0) {
			start();
		}
		wasHolding = holding;

		if (!gliding) {
			return;
		}

		// Planeur désélectionné, sol, eau, lave, échelle... : fin du vol
		if (!holding || mustStop(player)) {
			stop();
			return;
		}

		glide(client, player);
	}

	// Les situations où l'on ne peut pas (ou plus) planer
	private static boolean mustStop(LocalPlayer player) {
		return !player.isAlive()
				|| player.onGround()
				|| player.isInWater()
				|| player.isInLava()
				|| player.onClimbable()           // échelles, lianes...
				|| player.getAbilities().flying   // vol du mode créatif
				|| player.isFallFlying()          // élytres
				|| player.isPassenger()           // sur un cheval, dans un bateau...
				|| player.isSpectator();
	}

	private static void start() {
		gliding = true;
		// On prévient le serveur, qui préviendra les autres joueurs (pour dessiner la toile)
		ClientPlayNetworking.send(new GliderNetwork.GlidingPayload(true));
		// Le son du vent démarre (s'il ne joue pas déjà)
		if (sound == null || sound.isStopped()) {
			sound = new GliderSound();
			Minecraft.getInstance().getSoundManager().play(sound);
		}
	}

	private static void stop() {
		gliding = false;
		ClientPlayNetworking.send(new GliderNetwork.GlidingPayload(false));
	}

	// Un tick de vol : on fixe la vitesse du joueur
	private static void glide(Minecraft client, LocalPlayer player) {
		// Direction du regard, à plat : en Minecraft, yaw 0 = sud (+Z), yaw 90 = ouest (-X)
		double yaw = Math.toRadians(player.getYRot());
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		// La droite du joueur, perpendiculaire à la direction du regard
		double rightX = -forwardZ;
		double rightZ = forwardX;

		// Touches réglées dans les options du jeu (Q / D / S en AZERTY)
		double side = 0;
		if (client.options.keyLeft.isDown()) {
			side -= 1;
		}
		if (client.options.keyRight.isDown()) {
			side += 1;
		}
		boolean diving = client.options.keyDown.isDown();

		double motionX = forwardX * FORWARD_SPEED + rightX * side * SIDE_SPEED;
		double motionZ = forwardZ * FORWARD_SPEED + rightZ * side * SIDE_SPEED;

		// Vertical : on garde l'élan vers le haut du double saut, mais on ne tombe
		// jamais plus vite que la descente prévue (douce, ou en piqué avec S)
		double maxFall = diving ? DIVE_SPEED : SINK_SPEED;
		double motionY = Math.max(player.getDeltaMovement().y, -maxFall);

		player.setDeltaMovement(new Vec3(motionX, motionY, motionZ));
		player.resetFallDistance();
	}
}
