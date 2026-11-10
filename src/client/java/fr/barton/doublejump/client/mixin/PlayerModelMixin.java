package fr.barton.doublejump.client.mixin;

import fr.barton.doublejump.client.glider.GliderPlayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pose du joueur en planeur (il est couché à plat ventre, voir AvatarRendererMixin) :
 * bras tendus vers l'avant pour tenir la barre de contrôle, jambes droites, tête relevée.
 * Ce code s'exécute à la fin du calcul normal de la pose (marche, attaque...) et le remplace.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	// Angles en radians (3.14 = demi-tour)
	// Bras tendus "au-dessus de la tête" (donc vers l'avant, puisque le corps est couché),
	// et un peu vers le bas pour attraper la barre de contrôle
	private static final float ARM_FORWARD = (float) -Math.PI + 0.3F;
	private static final float ARM_SPREAD = 0.1F;              // bras un peu écartés
	private static final float HEAD_UP = (float) (-Math.PI / 4); // tête relevée pour regarder devant (comme en élytres)

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void doublejump$gliderPose(AvatarRenderState state, CallbackInfo ci) {
		if (!GliderPlayers.isGliding(state.id)) {
			return;
		}
		PlayerModel model = (PlayerModel) (Object) this;

		// Bras vers la barre (les manches suivent toutes seules : ce sont des "enfants" des bras)
		model.rightArm.xRot = ARM_FORWARD;
		model.rightArm.yRot = 0.0F;
		model.rightArm.zRot = -ARM_SPREAD;
		model.leftArm.xRot = ARM_FORWARD;
		model.leftArm.yRot = 0.0F;
		model.leftArm.zRot = ARM_SPREAD;

		// Jambes droites, dans le prolongement du corps : on ne "marche" plus dans les airs
		model.rightLeg.xRot = 0.0F;
		model.rightLeg.yRot = 0.0F;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.xRot = 0.0F;
		model.leftLeg.yRot = 0.0F;
		model.leftLeg.zRot = 0.0F;

		// Tête relevée (le chapeau de la peau suit tout seul)
		model.head.xRot = HEAD_UP;
	}
}
