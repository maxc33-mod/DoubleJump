package fr.barton.doublejump.client.glider;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Une "couche" ajoutée au dessin des joueurs (comme la cape ou l'armure) : le deltaplane, pendant le vol.
 * Il est fait de blocs du jeu étirés : une toile triangulaire en laine blanche, un trapèze
 * (deux montants + une barre de contrôle) en fer, et une sangle en laine noire.
 *
 * Repère utilisé (celui du corps du joueur, qui est couché à plat ventre pendant le vol) :
 * - X : de gauche à droite ;
 * - Y : le long du corps. 0 = les épaules, -0.5 = le sommet de la tête, +1.5 = les pieds.
 *   Les Y négatifs sont donc VERS L'AVANT ;
 * - Z : l'épaisseur du corps. Les Z positifs (le dos) sont VERS LE HAUT, les négatifs (le ventre) vers le bas.
 */
public class GliderSailLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

	// --- La toile (triangle isocèle, pointe vers l'avant) ---
	private static final float SAIL_WIDTH = 3.0F;      // largeur à l'arrière, en blocs
	private static final float SAIL_LENGTH = 2.0F;     // de la pointe à l'arrière
	private static final float SAIL_NOSE = -1.2F;      // position de la pointe (devant la tête)
	private static final float SAIL_HEIGHT = 0.9F;     // hauteur au-dessus du corps
	private static final float SAIL_THICKNESS = 1.0F / 16; // épaisseur d'un tapis
	private static final int SAIL_STRIPS = 16;         // plus de bandes = bords plus lisses

	// --- Le trapèze ---
	private static final float ROD = 0.06F;            // épaisseur des tubes
	// La barre de contrôle, là où arrivent les mains (bras tendus vers l'avant et un peu vers le bas)
	private static final float BAR_HALF_WIDTH = 0.6F;
	private static final float BAR_Y = -0.62F;
	private static final float BAR_Z = -0.22F;
	// Le sommet du trapèze, sous la toile, au-dessus des épaules
	private static final float APEX_HALF_WIDTH = 0.08F;
	private static final float APEX_Y = -0.15F;

	// --- La sangle qui relie le dos du pilote à la toile ---
	private static final float STRAP_Y = 0.3F;         // milieu du dos
	private static final float BACK_Z = 0.125F;        // surface du dos

	private final BlockModelResolver blockModelResolver;
	// Les modèles des blocs utilisés, préparés au premier dessin
	private final BlockModelRenderState whiteWool = new BlockModelRenderState();
	private final BlockModelRenderState iron = new BlockModelRenderState();
	private final BlockModelRenderState blackWool = new BlockModelRenderState();

	public GliderSailLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, BlockModelResolver blockModelResolver) {
		super(renderer);
		this.blockModelResolver = blockModelResolver;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
					   AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || !GliderPlayers.isGliding(state.id)) {
			return;
		}

		// En 26.3, laines et tapis sont rangés par couleur : Blocks.WOOL.white()
		if (whiteWool.isEmpty()) {
			blockModelResolver.update(whiteWool, Blocks.WOOL.white().defaultBlockState(), DISPLAY_CONTEXT);
			blockModelResolver.update(iron, Blocks.IRON_BLOCK.defaultBlockState(), DISPLAY_CONTEXT);
			blockModelResolver.update(blackWool, Blocks.WOOL.black().defaultBlockState(), DISPLAY_CONTEXT);
		}

		Drawer drawer = new Drawer(poseStack, submitNodeCollector, lightCoords,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);

		// 1. La toile : des bandes de plus en plus larges, de la pointe vers l'arrière
		float stripLength = SAIL_LENGTH / SAIL_STRIPS;
		for (int i = 0; i < SAIL_STRIPS; i++) {
			float halfWidth = SAIL_WIDTH * (i + 1) / SAIL_STRIPS / 2;
			float front = SAIL_NOSE + i * stripLength;
			drawer.box(whiteWool,
					-halfWidth, front, SAIL_HEIGHT,
					halfWidth, front + stripLength, SAIL_HEIGHT + SAIL_THICKNESS);
		}

		// 2. La barre de contrôle, tenue par les mains
		drawer.box(iron,
				-BAR_HALF_WIDTH, BAR_Y - ROD / 2, BAR_Z - ROD / 2,
				BAR_HALF_WIDTH, BAR_Y + ROD / 2, BAR_Z + ROD / 2);

		// 3. Les deux montants, des bouts de la barre jusqu'au sommet sous la toile
		drawer.rod(iron, -BAR_HALF_WIDTH, BAR_Y, BAR_Z, -APEX_HALF_WIDTH, APEX_Y, SAIL_HEIGHT);
		drawer.rod(iron, BAR_HALF_WIDTH, BAR_Y, BAR_Z, APEX_HALF_WIDTH, APEX_Y, SAIL_HEIGHT);

		// 4. La sangle du harnais, du dos jusqu'à la toile
		drawer.rod(blackWool, 0.0F, STRAP_Y, BACK_Z, 0.0F, STRAP_Y, SAIL_HEIGHT);
	}

	/** Petit outil pour dessiner des blocs étirés en pavés ou en tubes. */
	private record Drawer(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline) {

		/** Un pavé aligné sur les axes, d'un coin (x1, y1, z1) au coin opposé (x2, y2, z2). */
		void box(BlockModelRenderState block, float x1, float y1, float z1, float x2, float y2, float z2) {
			poseStack.pushPose();
			poseStack.translate(x1, y1, z1);
			poseStack.scale(x2 - x1, y2 - y1, z2 - z1); // le bloc (1 x 1 x 1) prend la taille du pavé
			block.submit(poseStack, collector, light, overlay, outline);
			poseStack.popPose();
		}

		/** Un tube fin, en ligne droite d'un point à un autre (dans n'importe quelle direction). */
		void rod(BlockModelRenderState block, float x1, float y1, float z1, float x2, float y2, float z2) {
			Vector3f direction = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
			float length = direction.length();

			poseStack.pushPose();
			poseStack.translate(x1, y1, z1);
			// On tourne le repère pour que son axe Y pointe vers l'autre bout du tube
			poseStack.rotate(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction.normalize()));
			poseStack.scale(ROD, length, ROD);
			poseStack.translate(-0.5F, 0.0F, -0.5F); // le tube est centré sur la ligne
			block.submit(poseStack, collector, light, overlay, outline);
			poseStack.popPose();
		}
	}
}
