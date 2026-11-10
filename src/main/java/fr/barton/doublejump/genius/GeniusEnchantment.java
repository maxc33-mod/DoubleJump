package fr.barton.doublejump.genius;

import fr.barton.doublejump.DoubleJump;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * L'enchantement "Éclair de génie".
 * Il est décrit dans le fichier data/doublejump/enchantment/stroke_of_genius.json :
 * ici, on sait seulement le retrouver et vérifier si une épée le possède.
 */
public final class GeniusEnchantment {
	/** L'identifiant de l'enchantement : doublejump:stroke_of_genius. */
	public static final ResourceKey<Enchantment> KEY =
			ResourceKey.create(Registries.ENCHANTMENT, DoubleJump.id("stroke_of_genius"));

	private GeniusEnchantment() {
	}

	/** Indique si cet objet est une épée portant l'Éclair de génie. */
	public static boolean isOn(Level level, ItemStack stack) {
		if (stack.isEmpty() || !stack.is(ItemTags.SWORDS)) {
			return false;
		}
		// Les enchantements sont rangés dans un registre propre à chaque monde
		Optional<Holder.Reference<Enchantment>> enchantment =
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(KEY);
		return enchantment.isPresent()
				&& EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack) > 0;
	}
}
