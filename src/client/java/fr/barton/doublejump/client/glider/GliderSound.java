package fr.barton.doublejump.client.glider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Le bruit du vent pendant le vol : le son des élytres, en plus doux.
 * Il suit le joueur et s'arrête tout seul à la fin du vol.
 */
public class GliderSound extends AbstractTickableSoundInstance {
	private static final float VOLUME = 0.4F;

	public GliderSound() {
		super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, RandomSource.create());
		this.looping = true;   // le son recommence en boucle
		this.delay = 0;
		this.volume = VOLUME;
		this.relative = false;
		followPlayer();
	}

	// Appelé 20 fois par seconde par le gestionnaire de sons
	@Override
	public void tick() {
		if (!GliderFlight.isGliding() || Minecraft.getInstance().player == null) {
			stop();
			return;
		}
		followPlayer();
	}

	// Le son se place à la position du joueur
	private void followPlayer() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			this.x = player.getX();
			this.y = player.getY();
			this.z = player.getZ();
		}
	}
}
