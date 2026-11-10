package fr.barton.doublejump.client.mixin;

import fr.barton.doublejump.client.glider.GliderFlight;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vue à la 1re personne : pendant le vol, les bras sont levés vers la barre du planeur,
 * donc on ne dessine ni les mains ni le planeur en bas de l'écran.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void doublejump$hideHandsWhileGliding(CallbackInfo ci) {
		if (GliderFlight.isGliding()) {
			ci.cancel(); // on saute tout le dessin des mains
		}
	}
}
