package fr.barton.doublejump.mixin;

import fr.barton.doublejump.zone.SafeZoneSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuse l'œuf de spawn dans une safe zone, sans le consommer.
 */
@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {
	@Inject(method = "spawnMob", at = @At("HEAD"), cancellable = true)
	private static void doublejump$refuseInSafeZones(EntityType<?> type, LivingEntity user, ItemStack stack,
			ServerLevel level, BlockPos pos, boolean alignPosition, boolean invertY,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (SafeZoneSpawns.isBlocked(level, type, pos)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
