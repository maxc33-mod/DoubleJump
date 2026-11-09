package fr.barton.doublejump.client.map;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WorldMapData {

    public static final int UNKNOWN_HEIGHT = Integer.MIN_VALUE;

    private final Map<Long, MapChunkData> chunks = new HashMap<>();
    private int version = 0; // augmente à chaque changement réel

    // Transforme les coordonnées (cx, cz) d'un chunk en un seul nombre unique
    public static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public void put(int cx, int cz, MapChunkData data) {
        MapChunkData old = chunks.get(key(cx, cz));
        if (old != null
                && Arrays.equals(old.colors, data.colors)
                && Arrays.equals(old.heights, data.heights)) {
            return; // rien n'a changé, on ne touche à rien
        }
        chunks.put(key(cx, cz), data);
        version++;
    }

    public MapChunkData get(int cx, int cz) {
        return chunks.get(key(cx, cz));
    }

    // Couleur ARGB du bloc le plus haut en (x, z), ou 0 si inconnu
    public int getColor(int x, int z) {
        MapChunkData chunk = chunks.get(key(x >> 4, z >> 4));
        if (chunk == null) {
            return 0;
        }
        return chunk.colors[((z & 15) << 4) | (x & 15)];
    }

    // Hauteur du bloc le plus haut en (x, z), ou UNKNOWN_HEIGHT si inconnu
    public int getHeight(int x, int z) {
        MapChunkData chunk = chunks.get(key(x >> 4, z >> 4));
        if (chunk == null) {
            return UNKNOWN_HEIGHT;
        }
        return chunk.heights[((z & 15) << 4) | (x & 15)];
    }

    // Copie de la liste des chunks, pour pouvoir la sauvegarder en arrière-plan
    public Map<Long, MapChunkData> snapshot() {
        return new HashMap<>(chunks);
    }

    // Ajoute des chunks chargés depuis le disque
    public void addAll(Map<Long, MapChunkData> loaded) {
        chunks.putAll(loaded);
        version++;
    }

    public int version() {
        return version;
    }

    public int chunkCount() {
        return chunks.size();
    }

    public void clear() {
        chunks.clear();
        version++;
    }
}