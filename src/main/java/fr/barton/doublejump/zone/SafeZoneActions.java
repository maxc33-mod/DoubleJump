package fr.barton.doublejump.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Les actions demandées depuis l'écran de la Magnétite.
 * Le serveur revérifie tout : on ne fait jamais confiance à ce qu'envoie un client.
 */
public final class SafeZoneActions {
	// Distance maximale entre le joueur et la Magnétite (un peu plus large que la portée normale).
	private static final double MAX_DISTANCE = 10.0;

	private SafeZoneActions() {
	}

	/** Renvoie la main qui tient une boussole (main droite d'abord), ou null s'il n'y en a pas. */
	public static InteractionHand findCompassHand(Player player) {
		if (player.getMainHandItem().is(Items.COMPASS)) {
			return InteractionHand.MAIN_HAND;
		}
		if (player.getOffhandItem().is(Items.COMPASS)) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	/** Indique si le joueur a de quoi payer cet agrandissement (gratuit en mode créatif). */
	public static boolean canPay(Player player, int cost) {
		return player.hasInfiniteMaterials() || player.getInventory().countItem(Items.QUARTZ) >= cost;
	}

	/** Associe la boussole tenue en main, en réutilisant le comportement normal du jeu. */
	public static void linkCompass(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();
		InteractionHand hand = findCompassHand(player);
		if (hand == null || !isUsable(player, level, pos)) {
			return;
		}

		// On simule un clic de la boussole sur le dessus de la Magnétite.
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		player.getItemInHand(hand).useOn(new UseOnContext(player, hand, hit));
	}

	/**
	 * Agrandit la zone d'un niveau contre du quartz.
	 * expectedLevel est le niveau affiché à l'écran du joueur : un double-clic
	 * n'agrandit donc qu'une fois et ne dépense pas le quartz du niveau suivant.
	 */
	public static void upgrade(ServerPlayer player, BlockPos pos, int expectedLevel) {
		ServerLevel level = player.level();
		if (!isUsable(player, level, pos)) {
			return;
		}

		SafeZone zone = SafeZoneData.get(level).find(pos);
		if (zone == null || zone.level() != expectedLevel || zone.isMaxLevel()
				|| !canPay(player, zone.upgradeCost())) {
			return;
		}

		if (!player.hasInfiniteMaterials()) {
			removeQuartz(player.getInventory(), zone.upgradeCost());
		}
		SafeZoneData.upgradeAt(level, zone);
	}

	// Le joueur est-il assez près, et la Magnétite est-elle toujours là ?
	private static boolean isUsable(ServerPlayer player, ServerLevel level, BlockPos pos) {
		return player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_DISTANCE * MAX_DISTANCE
				&& level.getBlockState(pos).is(Blocks.LODESTONE);
	}

	// Retire le quartz, case par case, jusqu'à atteindre la quantité demandée.
	private static void removeQuartz(Inventory inventory, int amount) {
		int remaining = amount;
		for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(Items.QUARTZ)) {
				int taken = Math.min(remaining, stack.getCount());
				stack.shrink(taken);
				remaining -= taken;
			}
		}
		inventory.setChanged();
	}
}
