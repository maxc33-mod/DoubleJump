# DoubleJump

Mod Fabric pour **Minecraft Java 26.3** qui rend l'exploration plus agréable : double saut, carte du monde, zones de sécurité, téléportation, un enchantement d'épée et un planeur.

*English version below.*

---

## 🇫🇷 Fonctionnalités

### Double saut et chutes sans dégâts
- Appuie une deuxième fois sur **Espace** en l'air pour faire un double saut (50 % plus haut qu'un saut normal).
- Les chutes ne font plus de dégâts.

### Carte du monde
- Touche **M** sur un clavier AZERTY (`;` en QWERTY), modifiable dans *Options → Commandes*.
- Carte en couleurs avec le relief, construite au fil de l'exploration et sauvegardée sur le disque.
- Une carte par monde et par dimension (Surface, Nether, End).
- **Molette** : zoom. **Glisser** : déplacer la carte. Bouton **« Me recentrer »**.
- Affiche ta position et ta direction, le point d'apparition et les joueurs proches.
- **Clic droit** : créer ou modifier un **waypoint** (nom et couleur).

### Zones de sécurité (Magnétite)
- Chaque **Magnétite** (*Lodestone*) posée par un joueur crée une zone de sécurité : un cube centré sur le bloc où les monstres hostiles n'apparaissent pas et n'entrent pas.
- Les monstres qui s'approchent sont repoussés ; ceux déjà présents disparaissent (sauf ceux nommés avec une étiquette).
- **Clic droit** sur la Magnétite : associer la boussole tenue en main, ou agrandir la zone avec du **quartz du Nether** :

  | Niveau | Côté du cube | Coût |
  |---|---|---|
  | 1 | 51 blocs | — |
  | 2 | 101 blocs | 50 quartz |
  | 3 | 151 blocs | 100 quartz |
  | 4 | 201 blocs | 150 quartz |

- Un halo bleu clair montre les limites de la zone ; elle apparaît aussi sur la carte.
- **Shift + clic droit** garde le comportement normal (poser un bloc contre la Magnétite).

### Téléportation
- Dans la carte, **clic gauche** sur l'icône d'une Magnétite, puis **« Se téléporter »**.
- Gratuit et sans délai, mais seulement **depuis l'intérieur d'une zone de sécurité**, et vers une Magnétite de la même dimension.

### Enchantement « Éclair de génie »
- Enchantement d'épée : à chaque coup chargé, un petit éclair part en ligne droite (20 blocs) et inflige les dégâts de l'épée au premier mob touché.
- Compatible avec Tranchant, Châtiment, Fléau des arthropodes, Recul et Aura de feu.
- Se trouve en livre dans les coffres et chez les bibliothécaires (pas à la table d'enchantement).

### Planeur
- Recette (débloquée dès que tu as du papier ou de la redstone) :

  ```
  Papier   Papier    Papier
  Fil      Redstone  Fil
  Bâton    (vide)    Bâton
  ```

- Planeur en main, fais un **double saut** pour décoller. Le prendre en main pendant une chute lance aussi le vol.
- En vol, tu avances vers où tu regardes en descendant doucement. **Q / D** : glisser sur le côté. **S** : piquer.
- Le vol s'arrête au sol, dans l'eau, dans la lave ou sur une échelle.
- En vue à la 3ᵉ personne, ton personnage est couché sous un deltaplane, visible aussi des autres joueurs.

## Installation

1. Installe **[Fabric Loader](https://fabricmc.net/use/installer/)** 0.20.1 ou plus récent pour Minecraft 26.3.
2. Place **[Fabric API](https://modrinth.com/mod/fabric-api)** (0.165.2+26.3 ou plus récent) et `doublejump-x.y.z.jar` dans le dossier `mods` (`%APPDATA%\.minecraft\mods` sous Windows).
3. Lance le jeu avec le profil Fabric.

Pour jouer en multijoueur, le mod doit être installé **sur le serveur et sur chaque client**.

## Compiler le mod

Il faut Java 25.

```
./gradlew build
```

Le fichier du mod se trouve ensuite dans `build/libs/`.

---

## 🇬🇧 English

A Fabric mod for **Minecraft Java 26.3**.

- **Double jump** (press Space again in mid-air) and **no fall damage**.
- **World map** (`;` on QWERTY, `M` on AZERTY): colours and relief, zoom, drag, recenter, players and spawn, one map per world and dimension, **waypoints** with right-click.
- **Lodestone safe zones**: every Lodestone placed by a player creates a cube where hostile mobs don't spawn and can't enter. Upgrade it with Nether quartz (51 → 101 → 151 → 201 blocks wide).
- **Teleportation**: left-click a Lodestone on the map to teleport next to it, from inside any safe zone, in the same dimension.
- **Stroke of Genius** sword enchantment: each fully charged swing fires a small lightning bolt that deals the sword's damage to the first mob hit (20 blocks range). Found in loot chests and from librarians.
- **Glider**: crafted from paper, string, redstone and sticks. Double jump while holding it to take off; glide forward, strafe with left/right, dive with back. Other players see you lying under a hang glider.

**Installation:** Fabric Loader 0.20.1+, Fabric API 0.165.2+26.3, on both server and clients.

## License

[CC0 1.0](LICENSE)
