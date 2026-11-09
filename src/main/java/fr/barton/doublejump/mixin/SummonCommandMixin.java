package fr.barton.doublejump.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.barton.doublejump.zone.SafeZoneSpawns;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Laisse passer la commande /summon dans les safe zones (pratique pour les tests).
 */
@Mixin(SummonCommand.class)
public class SummonCommandMixin {
	// Entoure l'ajout de l'entité : l'autorisation ne vaut que le temps de cet appel.
	@WrapOperation(method = "createEntity", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
	private static boolean doublejump$allowCommandSpawn(ServerLevel level, Entity entity, Operation<Boolean> original) {
		return SafeZoneSpawns.runCommandSpawn(() -> original.call(level, entity));
	}
}
