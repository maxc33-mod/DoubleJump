package fr.barton.doublejump.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.barton.doublejump.client.glider.GliderPlayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le dessin des joueurs en planeur (vue à la 3e personne et autres joueurs) :
 * - les objets tenus en main sont cachés (les mains tiennent la barre) ;
 * - le corps est couché à plat ventre, comme sur un deltaplane.
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	private static final float BODY_HEIGHT = 1.2F;      // hauteur du corps couché au-dessus des pieds, en blocs
	private static final float HALF_BODY_LENGTH = 0.9F; // moitié de la longueur du corps

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void doublejump$hideItemsWhileGliding(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		if (GliderPlayers.isGliding(state.id)) {
			state.rightHandItemState.clear();
			state.leftHandItemState.clear();
			state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
			state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
		}
	}

	// Appelé après l'orientation normale du corps (le joueur regarde dans sa direction)
	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
			at = @At("TAIL"))
	private void doublejump$lieDownWhileGliding(AvatarRenderState state, PoseStack poseStack, float bodyRot,
												float entityScale, CallbackInfo ci) {
		if (GliderPlayers.isGliding(state.id)) {
			poseStack.translate(0.0F, BODY_HEIGHT, 0.0F);       // on remonte le corps à mi-hauteur
			poseStack.rotateDegrees(Axis.XP, -90.0F);            // on le bascule à plat ventre, tête en avant
			poseStack.translate(0.0F, -HALF_BODY_LENGTH, 0.0F); // on le recule pour qu'il soit centré
		}
	}
}
