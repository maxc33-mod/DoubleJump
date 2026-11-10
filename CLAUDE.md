# Mod DoubleJump (Fabric, Minecraft Java 26.3)

## Projet
- Mod Fabric pour Minecraft Java **26.3** (Java 25, Fabric Loader 0.20.1+, Fabric API 0.165.2+26.3).
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
- `zone/` (main) : `SafeZone` (position + niveau), `SafeZoneData` (zones d'une dimension, sauvegardées avec le monde), `SafeZoneActions` (boussole et quartz, vérifiés côté serveur), `SafeZoneSpawns` (règle d'apparition des monstres), `SafeZoneGuard` (disparition à la création ou à l'agrandissement, monstres repoussés dans la bande de 2 blocs à l'intérieur de la limite).
- `network/SafeZoneNetwork.java` (main) : messages réseau des zones et synchronisation vers les clients.
- `client/LodestoneScreen.java` : écran de la Magnétite. `client/TeleportScreen.java` : écran de téléportation ouvert depuis la carte. `client/zone/ClientSafeZones.java` : copie des zones côté client. `client/zone/SafeZoneHalo.java` : halo de particules.
- `mixin/` (main) : `BlockItemMixin` (pose d'une Magnétite par un joueur), `LevelChunkMixin` (disparition d'une Magnétite), `ServerLevelMixin` (blocage des apparitions), `SpawnEggItemMixin` (œuf refusé sans être consommé), `SummonCommandMixin` (`/summon` autorisé).
- `client/DoubleJumpClient.java` : point d'entrée client, double saut, touche de la carte, scan, sauvegarde à la fermeture.
- `client/MapScreen.java`, `client/WaypointEditScreen.java` : écrans.
- `genius/` (main) : `GeniusEnchantment` (clé de l'enchantement, test sur une épée), `GeniusBolt` (un éclair en vol, simulé côté serveur sans entité), `GeniusBolts` (lancers vérifiés, éclairs actifs). `network/GeniusBoltNetwork.java` : message de lancer. `client/genius/GeniusBoltInput.java` : détection du coup d'épée chargé.
- Données : `data/doublejump/enchantment/stroke_of_genius.json`, tags `data/minecraft/tags/enchantment/` (`treasure`, `tradeable`, `on_random_loot`).
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
- Données de monde : `SavedDataType(id, constructeur, codec, dataFixTypes)` avec `level.getDataStorage().computeIfAbsent(TYPE)`. Le `DataFixTypes` ne doit jamais être null (utilisé sans vérification à la relecture du fichier).
- Événement de chargement de dimension : `ServerLevelEvents.LOAD` (ancien `ServerWorldEvents`).
- Réseau Fabric : `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()` (anciens `playS2C` / `playC2S`). Changement de dimension : `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`.
- `UseBlockCallback` côté client : renvoyer `FAIL` annule le clic sans rien envoyer au serveur.
- Types d'entités : constantes dans `EntityTypes` (ex. `EntityTypes.ZOMBIFIED_PIGLIN`), et non plus dans `EntityType`.
- Toute nouvelle entité passe par `ServerLevel.addFreshEntity` (apparitions naturelles, spawners, œufs, `/summon`).
- Un écran qui envoie des demandes au serveur doit avoir `isPauseScreen()` à `false`, sinon le serveur solo est en pause.

## Fonctionnalité de la v2 (terminée et testée) : zones de sécurité autour de la Magnétite
La "Magnétite" est le nom français du bloc vanilla `minecraft:lodestone` (Lodestone).
Chaque Magnétite posée par un joueur crée une safe zone : un cube centré sur le bloc, où les monstres hostiles n'apparaissent pas et n'entrent pas.
Cahier des charges validé par l'utilisateur le 2026-11-09 :
- **Quelles Magnétites :** uniquement celles posées par un joueur une fois le mod installé. Les autres gardent le comportement vanilla.
- **Clic droit :** ouvre toujours l'écran, même boussole en main (remplace l'association directe). Shift + clic droit garde le comportement normal (ex. poser un bloc contre la Magnétite).
- **Écran, deux choix :** associer la boussole tenue en main (bouton grisé si aucune) / agrandir la zone avec du quartz.
- **Niveaux (côté du cube, identique en hauteur) :** niveau 1 = 51 (initial), niveau 2 = 101 (50 quartz du Nether), niveau 3 = 151 (100 quartz), niveau 4 = 201 (150 quartz). Le niveau 4 est le maximum.
- **Plusieurs zones :** chaque Magnétite garde sa propre zone ; la surface protégée est la réunion des zones (une Magnétite posée dans une zone existante étend la protection).
- **Monstres concernés :** les monstres toujours hostiles. Exclus : boss (Wither, Dragon) et neutres (Endermen, Piglins, Piglins zombifiés).
- **Comportement :** les monstres qui entrent sont repoussés en bordure ; ceux déjà dedans disparaissent, sauf ceux nommés avec une étiquette. Apparitions naturelles, spawners et œufs de spawn bloqués ; les commandes ne le sont pas (pour les tests).
- **Limite et accès :** pas de limite de zones ; n'importe quel joueur peut agrandir ou détruire une zone. L'agrandissement est gratuit en mode créatif. Une Magnétite détruite ne rend pas le quartz.
- **Dimensions :** Overworld, Nether et End.
- **Halo :** particules bleu très clair, visibles seulement à moins de ~10 blocs d'une face du cube.
- **Carte :** cube bleu translucide avec une icône au centre.
- **Technique :** niveau stocké côté serveur (données du monde, par dimension et position), synchronisé aux clients par messages réseau. Le mod doit être installé côté serveur ET côté client.

## Fonctionnalité de la v3 (terminée et testée) : téléportation vers une Magnétite
Cahier des charges validé par l'utilisateur le 2026-11-10 :
- Dans la carte, clic gauche sur l'icône d'une Magnétite : écran avec un bouton « Se téléporter » et un bouton « Annuler ».
- Gratuit, sans délai de recharge, pour tous les joueurs et toutes les Magnétites.
- Départ autorisé uniquement depuis l'intérieur d'une safe zone (n'importe laquelle). Bouton grisé sinon (et vérifié côté serveur).
- Même dimension uniquement.
- Arrivée : case sûre à côté de la Magnétite (8 cases autour, puis un cran plus loin, hauteurs -2 à +2), sinon sur la Magnétite. Message si aucune place.
- Technique : `SafeZoneNetwork.TeleportPayload` (client vers serveur), `SafeZoneActions.teleport` (vérifications + `player.teleportTo(x, y, z)`).

## Fonctionnalité de la v4 (terminée et testée) : enchantement « Éclair de génie » (Stroke of Genius)
Cahier des charges validé par l'utilisateur le 2026-11-10 :
- Épées uniquement, un seul niveau, compatible avec tous les enchantements d'épée.
- À chaque coup (même dans le vide), si la barre d'attaque est chargée à plus de 90 % : un petit éclair part en ligne droite depuis les yeux du joueur. Pas de tir quand on vise un bloc (on creuse).
- Vitesse 3 blocs/tick, portée 20 blocs, s'arrête au premier mob touché, disparaît contre un bloc sans rien casser.
- Dégâts : dégâts de base de l'épée + Tranchant/Châtiment/Fléau des arthropodes, sans critique ni potion de Force.
- Cibles : tout ce qu'une épée peut toucher sauf le lanceur ; joueurs seulement si le PvP est permis. Le mob frappé au corps à corps par le même coup est ignoré par l'éclair (pas de double dégât).
- Pas de recul de base, mais l'enchantement Recul s'applique. Aura de feu s'applique. 1 point d'usure quand l'éclair touche.
- Apparence : traînée d'étincelles électriques (`ELECTRIC_SPARK`), balayage (`SWEEP_ATTACK`) à l'impact. Son : `TRIDENT_THUNDER` (Canalisation) au lancer et à l'impact.
- Obtention : livres dans les coffres et chez les bibliothécaires, pas à la table d'enchantement (comme Raccommodage).
- Particularités 26.3 : `ClientPreAttackCallback` (clic d'attaque, `clickCount > 0`), `LivingEntity.knockback(force, x, z, source, dégâts)`, `EnchantmentHelper.modifyDamage/modifyKnockback/doPostAttackEffectsWithItemSource`, `ItemAttributeModifiers.compute(attribut, base, slot)`.
