package fr.barton.doublejump.mixin;

import fr.barton.doublejump.zone.SafeZoneSpawns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Point de passage de toutes les nouvelles entités : apparitions naturelles, spawners, œufs...
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void doublejump$blockInSafeZones(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		ServerLevel level = (ServerLevel) (Object) this;
		if (SafeZoneSpawns.isBlocked(level, entity.getType(), entity.blockPosition())) {
			cir.setReturnValue(false); // l'entité n'est jamais ajoutée au monde
		}
	}
}
