package fr.barton.doublejump.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import fr.barton.doublejump.DoubleJump;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class MapRenderer {

    private static final int UNKNOWN_COLOR = 0xFF202020; // zone jamais explorée

    private final Identifier textureId = DoubleJump.id("map_view");
    private NativeImage image;
    private DynamicTexture texture;
    private int width;
    private int height;

    // Deux "mémoires" du dernier chunk consulté (une pour le bloc, une pour son voisin)
    private final ChunkCache cacheBlock = new ChunkCache();
    private final ChunkCache cacheNeighbour = new ChunkCache();

    // Mémoire pour savoir si l'image doit être redessinée
    private boolean forceRedraw = true;
    private int lastVersion = -1;
    private double lastCenterX = Double.NaN;
    private double lastCenterZ = Double.NaN;
    private double lastScale = Double.NaN;

    public Identifier textureId() {
        return textureId;
    }

    // Crée l'image (appelé à l'ouverture de l'écran et quand sa taille change)
    public void init(Minecraft client, int width, int height) {
        this.width = width;
        this.height = height;
        this.image = new NativeImage(width, height, false);
        this.texture = new DynamicTexture(() -> "doublejump map", this.image);
        client.getTextureManager().register(textureId, texture);
        this.forceRedraw = true;
    }

    // centerX/centerZ : coordonnées du monde au centre de l'écran
    // scale : nombre de pixels par bloc
    public void update(WorldMapData data, double centerX, double centerZ, double scale) {
        if (texture == null) {
            return;
        }
        boolean changed = forceRedraw
                || data.version() != lastVersion
                || centerX != lastCenterX
                || centerZ != lastCenterZ
                || scale != lastScale;
        if (!changed) {
            return;
        }

        forceRedraw = false;
        lastVersion = data.version();
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        lastScale = scale;

        draw(data, centerX, centerZ, scale);
        texture.upload(); // envoie l'image à la carte graphique
    }

    private void draw(WorldMapData data, double centerX, double centerZ, double scale) {
        cacheBlock.reset();
        cacheNeighbour.reset();

        for (int py = 0; py < height; py++) {
            int worldZ = (int) Math.floor(centerZ + (py - height / 2.0) / scale);
            for (int px = 0; px < width; px++) {
                int worldX = (int) Math.floor(centerX + (px - width / 2.0) / scale);
                image.setPixel(px, py, pixelColor(data, worldX, worldZ));
            }
        }
    }

    private int pixelColor(WorldMapData data, int x, int z) {
        MapChunkData chunk = cacheBlock.get(data, x >> 4, z >> 4);
        if (chunk == null) {
            return UNKNOWN_COLOR;
        }

        int index = ((z & 15) << 4) | (x & 15);
        int color = chunk.colors[index];
        if (color == 0) {
            return UNKNOWN_COLOR;
        }
        int height = chunk.heights[index];

        // Relief : on compare la hauteur avec le voisin nord-ouest.
        // Plus haut que lui = plus clair, plus bas = plus sombre.
        int nx = x - 1;
        int nz = z - 1;
        int neighbour = height;
        MapChunkData neighbourChunk = cacheNeighbour.get(data, nx >> 4, nz >> 4);
        if (neighbourChunk != null) {
            neighbour = neighbourChunk.heights[((nz & 15) << 4) | (nx & 15)];
        }

        int diff = Math.max(-4, Math.min(4, height - neighbour));
        return shade(color, 1.0 + diff * 0.09);
    }

    // Multiplie les composantes rouge, verte et bleue par un facteur
    private static int shade(int argb, double factor) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * factor));
        int g = clamp((int) (((argb >> 8) & 0xFF) * factor));
        int b = clamp((int) ((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    // Retient le dernier chunk consulté : des pixels voisins sont presque
    // toujours dans le même chunk, donc on évite de le rechercher à chaque fois.
    private static class ChunkCache {
        private int cx = Integer.MIN_VALUE;
        private int cz = Integer.MIN_VALUE;
        private MapChunkData chunk = null;

        void reset() {
            cx = Integer.MIN_VALUE;
            cz = Integer.MIN_VALUE;
            chunk = null;
        }

        MapChunkData get(WorldMapData data, int newCx, int newCz) {
            if (newCx != cx || newCz != cz) {
                chunk = data.get(newCx, newCz);
                cx = newCx;
                cz = newCz;
            }
            return chunk;
        }
    }
}