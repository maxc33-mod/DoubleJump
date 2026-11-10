package fr.barton.doublejump.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Les actions demandées depuis l'écran de la Magnétite et depuis la carte.
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

	/**
	 * Téléporte le joueur à côté d'une Magnétite de sa dimension.
	 * Gratuit, sans délai, mais seulement depuis l'intérieur d'une safe zone.
	 */
	public static void teleport(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();
		SafeZoneData data = SafeZoneData.get(level);

		// La liste ne contient que les zones de la dimension du joueur :
		// une Magnétite d'une autre dimension n'y est donc jamais trouvée.
		SafeZone zone = data.find(pos);
		if (zone == null) {
			return;
		}

		// Départ autorisé seulement depuis une safe zone (n'importe laquelle).
		if (!data.isProtected(player.blockPosition())) {
			player.sendSystemMessage(Component.translatable("message.doublejump.teleport.not_in_zone"));
			return;
		}

		BlockPos destination = findSafeSpot(level, zone.pos());
		if (destination == null) {
			player.sendSystemMessage(Component.translatable("message.doublejump.teleport.no_space"));
			return;
		}

		// Petit bruit de téléportation au départ...
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

		player.stopRiding(); // on descend d'un cheval ou d'un bateau avant de partir
		player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
		player.resetFallDistance();

		// ... et à l'arrivée.
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	/**
	 * Cherche une case où le joueur peut se tenir debout, à côté de la Magnétite :
	 * d'abord les 8 cases qui l'entourent, puis un cran plus loin, en essayant
	 * plusieurs hauteurs. En dernier recours, sur la Magnétite elle-même.
	 * Renvoie null si aucune case ne convient.
	 */
	private static BlockPos findSafeSpot(ServerLevel level, BlockPos lodestone) {
		int[] heights = {0, 1, -1, 2, -2};
		for (int distance = 1; distance <= 2; distance++) {
			for (int dy : heights) {
				for (int dx = -distance; dx <= distance; dx++) {
					for (int dz = -distance; dz <= distance; dz++) {
						// On ne garde que le "contour" du carré, à la bonne distance
						if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) {
							continue;
						}
						BlockPos candidate = lodestone.offset(dx, dy, dz);
						if (isSafeSpot(level, candidate)) {
							return candidate;
						}
					}
				}
			}
		}

		BlockPos onTop = lodestone.above();
		return isSafeSpot(level, onTop) ? onTop : null;
	}

	// Une case est sûre si les pieds et la tête sont libres, et le sol solide et sans danger.
	private static boolean isSafeSpot(ServerLevel level, BlockPos feet) {
		BlockPos head = feet.above();
		BlockPos ground = feet.below();
		return isFreeAndHarmless(level, feet)
				&& isFreeAndHarmless(level, head)
				&& !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()
				&& isHarmlessGround(level.getBlockState(ground));
	}

	// Ni bloc qui gêne, ni liquide, ni feu, ni buisson qui pique.
	private static boolean isFreeAndHarmless(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty()
				&& state.getFluidState().isEmpty()
				&& !state.is(BlockTags.FIRE)
				&& !state.is(Blocks.SWEET_BERRY_BUSH)
				&& !state.is(Blocks.POWDER_SNOW)
				&& !state.is(Blocks.COBWEB);
	}

	// Sol qui ne brûle pas et ne pique pas.
	private static boolean isHarmlessGround(BlockState state) {
		return !state.is(Blocks.MAGMA_BLOCK)
				&& !state.is(Blocks.CACTUS)
				&& !state.is(BlockTags.CAMPFIRES);
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
