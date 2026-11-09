package fr.barton.doublejump.mixin;

import fr.barton.doublejump.zone.SafeZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Détecte la disparition d'une Magnétite, quelle qu'en soit la cause
 * (joueur, explosion, commande...).
 */
@Mixin(LevelChunk.class)
public class LevelChunkMixin {
	// Exécuté après chaque changement de bloc. La valeur renvoyée est l'ancien bloc (ou null).
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void doublejump$afterSetBlockState(BlockPos pos, BlockState newState, int flags,
			CallbackInfoReturnable<BlockState> cir) {
		BlockState oldState = cir.getReturnValue();
		if (oldState != null && oldState.is(Blocks.LODESTONE) && !newState.is(Blocks.LODESTONE)
				&& ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
			SafeZoneData.removeAt(level, pos);
		}
	}
}
