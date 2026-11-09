package fr.barton.doublejump.mixin;

import fr.barton.doublejump.zone.SafeZoneData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Détecte la pose d'une Magnétite par un joueur.
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {
	// Exécuté juste après la tentative de pose d'un bloc depuis un objet.
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void doublejump$afterPlaceBlock(BlockPlaceContext context, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		// getPlayer() vaut null quand ce n'est pas un joueur qui pose (par exemple un distributeur).
		if (cir.getReturnValue() && state.is(Blocks.LODESTONE) && context.getPlayer() != null
				&& context.getLevel() instanceof ServerLevel level) {
			SafeZoneData.createAt(level, context.getClickedPos());
		}
	}
}
