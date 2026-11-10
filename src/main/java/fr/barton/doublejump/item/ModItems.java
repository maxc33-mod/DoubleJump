package fr.barton.doublejump.item;

import fr.barton.doublejump.DoubleJump;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Les objets ajoutés par le mod.
 */
public final class ModItems {
	/** L'identifiant du planeur : doublejump:glider. */
	public static final ResourceKey<Item> GLIDER_KEY = ResourceKey.create(Registries.ITEM, DoubleJump.id("glider"));

	/**
	 * Le planeur : un objet simple, éternel (pas d'usure), un seul par case d'inventaire.
	 * Tout son comportement (le vol) se trouve côté client, dans client/glider/.
	 */
	public static final Item GLIDER = Registry.register(BuiltInRegistries.ITEM, GLIDER_KEY,
			new Item(new Item.Properties().setId(GLIDER_KEY).stacksTo(1)));

	private ModItems() {
	}

	/** À appeler une seule fois au démarrage du mod (cela déclenche l'enregistrement ci-dessus). */
	public static void register() {
		// Le planeur apparaît dans l'onglet "Outils et utilitaires" du mode créatif
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(GLIDER));
	}
}
