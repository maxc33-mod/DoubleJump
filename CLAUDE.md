# Mod DoubleJump (Fabric, Minecraft Java 26.3)

## Projet
- Mod Fabric pour Minecraft Java **26.3** (Java 25, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3).
- Id du mod : `doublejump`. Package : `fr.barton.doublejump`.
- Minecraft est non obfusqué : on utilise les noms officiels Mojang.
- Sources séparées : `src/main` (commun/serveur) et `src/client` (client uniquement).
- Dossier de test lancé par `runClient` : `run/`.

## À propos de l'utilisateur
- Débutant en Java, francophone, clavier AZERTY.
- Répondre en français. Commenter le code en français, simplement.
- Toujours fournir des fichiers complets avec TOUS les imports.
- Après une modification, compiler avec `./gradlew build` et corriger les erreurs avant de dire que c'est fini.
- Expliquer brièvement ce que fait chaque nouveau morceau de code.

## Fonctionnalités de la v1 (terminées et testées)
1. Double saut (+50 % de hauteur : JUMP_STRENGTH = 0.514), côté client.
2. Suppression des dégâts de chute (`ServerLivingEntityEvents.ALLOW_DAMAGE`, côté serveur).
3. Carte plein écran (touche par défaut `KEY_SEMICOLON`, soit "M" sur AZERTY) :
   couleurs et relief, zoom molette, déplacement au glisser, bouton "Me recentrer",
   position et direction du joueur, coordonnées, spawn, autres joueurs proches,
   une carte par monde ET par dimension (Nether : scan depuis la hauteur du joueur),
   sauvegarde sur disque, waypoints (clic droit : nom + couleur, modifier, supprimer).

## Fichiers
- `DoubleJump.java` (main) : ModInitializer, `id()`, annulation des dégâts de chute.
- `client/DoubleJumpClient.java` : point d'entrée client, double saut, touche de la carte, scan, sauvegarde à la fermeture.
- `client/MapScreen.java`, `client/WaypointEditScreen.java` : écrans.
- `client/map/` : `MapChunkData`, `WorldMapData`, `MapScanner`, `MapRenderer`, `MapKeys`, `MapStorage`.
- `client/waypoint/` : `Waypoint`, `WaypointStorage`, `WaypointManager`.
- Traductions : `src/main/resources/assets/doublejump/lang/` (`fr_fr.json`, `en_us.json`).

## Stockage (dans `<dossier du jeu>/doublejump/maps/`)
- `<monde>/<dimension>.dat` : GZIP, version 1 (int version, int nombre de chunks, puis par chunk : cx, cz, 256 int couleurs, 256 short hauteurs).
- `<monde>/<dimension>.waypoints.json`.
- Identifiant de monde solo : fichier `doublejump_world_id.txt` (UUID) dans le dossier du monde. Multijoueur : adresse du serveur.

## Particularités de l'API en 26.3 (à vérifier à la compilation si un nom change)
- Écrans : `this.minecraft.gui.setScreen(...)`, écran actuel : `Minecraft.gui.screen()`.
- Rendu : `GuiGraphicsExtractor`. `extractRenderState(graphics, mouseX, mouseY, delta)`,
  `graphics.text(font, String, x, y, ARGB, ombre)`, `graphics.fill(...)`,
  `graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, regionW, regionH, texW, texH)` (forme à 12 arguments).
- Souris : numérotation SDL en 26.3. TOUJOURS comparer avec `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`, jamais avec un chiffre. Événements : `MouseButtonEvent` (`x()`, `y()`, `button()`).
- Touche : `new KeyMapping(cle, InputConstants.KEY_XXX, categorie)`, catégorie via `KeyMapping.Category.register(DoubleJump.id("main"))`, enregistrement via `KeyMappingHelper.registerKeyMapping`.
- Textures dynamiques : `new DynamicTexture(() -> "nom", nativeImage)`, `getTextureManager().register(id, texture)`, `texture.upload()`.
- Noms 26.x : `ResourceKey.identifier()` (ancien `location()`), `level.getRespawnData().pos()` (ancien `getSharedSpawnPos()`).
- `BlockState.blocksMotion()` n'est pas fiable : utiliser `getCollisionShape(level, pos).isEmpty()`.

## Prochaine fonctionnalité (v2) : zones de sécurité autour de la Magnétite
La "Magnétite" est le nom français du bloc vanilla `minecraft:lodestone` (Lodestone).
Idée : chaque Magnétite posée crée un cube de 51 blocs de côté où les monstres hostiles n'apparaissent pas et n'entrent pas.
- Clic droit sur la Magnétite : écran avec deux choix (associer la boussole tenue en main / agrandir la zone avec du quartz).
- Taille stockée côté serveur (données du monde, par position), synchronisée aux clients par messages réseau.
- Halo bleu très clair en particules à proximité des faces du cube.
- Les zones apparaissent sur la carte (cube bleu translucide).
- Le mod devra être installé côté serveur ET côté client.
  Les détails sont à valider avec l'utilisateur (voir ses réponses aux questions de cadrage).
