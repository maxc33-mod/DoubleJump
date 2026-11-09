package fr.barton.doublejump.client.map;

public class MapChunkData {
    // Index d'une colonne = z * 16 + x (positions locales dans le chunk, de 0 à 15)
    public final int[] colors = new int[256];   // couleur ARGB, 0 = rien à afficher
    public final int[] heights = new int[256];  // hauteur du bloc le plus haut (servira au relief)
}