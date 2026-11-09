package fr.barton.doublejump.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.barton.doublejump.DoubleJump;
import fr.barton.doublejump.client.map.MapScanner;
import fr.barton.doublejump.client.map.MapStorage;
import fr.barton.doublejump.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class DoubleJumpClient implements ClientModInitializer {

	// La mémoire de la carte et les waypoints, partagés avec les écrans
	public static final MapScanner SCANNER = new MapScanner();
	public static final WaypointManager WAYPOINTS = new WaypointManager();

	private static KeyMapping openMapKey;
	private boolean scannerBroken = false; // vrai si le scan a rencontré une erreur

	// Réglages du double saut
	private static final int MAX_EXTRA_JUMPS = 1;
	private static final double JUMP_STRENGTH = 0.514;

	// Mémoire entre deux ticks (double saut)
	private int extraJumpsUsed = 0;
	private boolean jumpWasDown = false;
	private boolean wasOnGround = true;

	@Override
	public void onInitializeClient() {
		// 1. La touche d'ouverture de la carte, dans la catégorie "Double Jump"
		KeyMapping.Category category = KeyMapping.Category.register(DoubleJump.id("main"));
		openMapKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping(
						"key.doublejump.open_map",
						InputConstants.KEY_SEMICOLON,
						category));

		// 2. Quand l'écran de la carte est ouvert, la même touche le referme
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof MapScreen) {
				ScreenKeyboardEvents.beforeKeyPress(screen).register((s, keyEvent) -> {
					if (openMapKey.matches(keyEvent)) {
						client.gui.setScreen(null);
					}
				});
			}
		});

		// 3. Chaque tick : double saut, touche de la carte, scan du monde
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

		// 4. À la fermeture du jeu : dernière sauvegarde de la carte
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			SCANNER.saveIfNeeded();
			MapStorage.finishAndStop();
		});
	}

	private void onTick(Minecraft client) {
		doubleJumpTick(client);
		mapTick(client);
	}

	private void mapTick(Minecraft client) {
		while (openMapKey.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) {
				client.gui.setScreen(new MapScreen());
			}
		}

		if (scannerBroken) {
			return;
		}
		try {
			SCANNER.tick(client);
		} catch (RuntimeException e) {
			scannerBroken = true;
			DoubleJump.LOGGER.error("Erreur dans le scan de la carte : le scan est désactivé", e);
		}
	}

	private void doubleJumpTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		boolean jumpDown = client.options.keyJump.isDown();
		boolean jumpJustPressed = jumpDown && !jumpWasDown;

		if (player.onGround()) {
			extraJumpsUsed = 0;
		} else if (jumpJustPressed
				&& !wasOnGround
				&& extraJumpsUsed < MAX_EXTRA_JUMPS
				&& !player.getAbilities().flying
				&& !player.isFallFlying()
				&& !player.isInWater()) {

			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(motion.x, JUMP_STRENGTH, motion.z);
			player.resetFallDistance();
			extraJumpsUsed++;
		}

		jumpWasDown = jumpDown;
		wasOnGround = player.onGround();
	}
}