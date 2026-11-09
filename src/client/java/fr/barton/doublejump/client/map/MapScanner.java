package fr.barton.doublejump.client.map;

import fr.barton.doublejump.DoubleJump;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public class MapScanner {

    private static final int RADIUS = 6;             // rayon du scan, en chunks
    private static final int SCAN_INTERVAL = 20;     // nouvelle série de scans environ chaque seconde
    private static final int CHUNKS_PER_TICK = 8;    // chunks scannés à chaque tick
    private static final int SAVE_INTERVAL = 1200;   // 1 sauvegarde par minute (1200 ticks)

    private final WorldMapData data = new WorldMapData();
    private final ArrayDeque<int[]> pending = new ArrayDeque<>(); // file des chunks à scanner

    private ClientLevel lastLevel = null;
    private String currentKey = null;  // fichier de la carte actuelle
    private int savedVersion = 0;      // version de la carte au dernier enregistrement
    private int scanCounter = 0;
    private int saveCounter = 0;

    public WorldMapData data() {
        return data;
    }

    // Identifiant du monde et de la dimension actuels (sert aussi aux waypoints)
    public String currentKey() {
        return currentKey;
    }

    public void tick(Minecraft client) {
        ClientLevel level = client.level;

        // On a quitté le monde (retour au menu, déconnexion...)
        if (level == null) {
            if (lastLevel != null) {
                leaveWorld();
            }
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        // Nouveau monde ou nouvelle dimension
        if (level != lastLevel) {
            enterLevel(client, level);
        }

        // Sauvegarde périodique
        saveCounter++;
        if (saveCounter >= SAVE_INTERVAL) {
            saveCounter = 0;
            saveIfNeeded();
        }

        // Quand la file est vide, on prépare une nouvelle série de chunks à scanner
        scanCounter++;
        if (scanCounter >= SCAN_INTERVAL && pending.isEmpty()) {
            scanCounter = 0;
            fillQueue(player);
        }

        // Dans le Nether (plafond de bedrock), on scanne depuis la hauteur du joueur
        boolean ceiling = Level.NETHER.equals(level.dimension());
        int startY = player.getBlockY() + 1;

        // On traite quelques chunks de la file à chaque tick
        int scanned = 0;
        while (scanned < CHUNKS_PER_TICK && !pending.isEmpty()) {
            int[] chunk = pending.poll();
            if (level.hasChunk(chunk[0], chunk[1])) {
                data.put(chunk[0], chunk[1], scanChunk(level, chunk[0], chunk[1], ceiling, startY));
            }
            scanned++;
        }
    }

    // Liste les chunks autour du joueur, des plus proches aux plus éloignés
    private void fillQueue(LocalPlayer player) {
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;

        List<int[]> chunks = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                chunks.add(new int[] {playerChunkX + dx, playerChunkZ + dz});
            }
        }
        chunks.sort(Comparator.comparingInt(
                c -> (c[0] - playerChunkX) * (c[0] - playerChunkX)
                        + (c[1] - playerChunkZ) * (c[1] - playerChunkZ)));
        pending.addAll(chunks);
    }

    // Sauvegarde la carte actuelle si elle a changé depuis la dernière fois
    public void saveIfNeeded() {
        if (currentKey != null && data.version() != savedVersion) {
            MapStorage.saveAsync(currentKey, data.snapshot());
            savedVersion = data.version();
        }
    }

    private void enterLevel(Minecraft client, ClientLevel level) {
        saveIfNeeded(); // on enregistre l'ancienne carte avant de la remplacer
        data.clear();
        pending.clear();

        currentKey = MapKeys.keyFor(client, level);
        data.addAll(MapStorage.load(currentKey));
        savedVersion = data.version();
        lastLevel = level;

        DoubleJump.LOGGER.info("Carte chargée : {} ({} chunks)", currentKey, data.chunkCount());
    }

    private void leaveWorld() {
        saveIfNeeded();
        data.clear();
        pending.clear();
        currentKey = null;
        lastLevel = null;
        savedVersion = data.version();
    }

    private MapChunkData scanChunk(ClientLevel level, int cx, int cz, boolean ceiling, int startY) {
        MapChunkData result = new MapChunkData();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = (cx << 4) + lx;
                int z = (cz << 4) + lz;

                int topY;
                if (ceiling) {
                    topY = findSurfaceBelow(level, pos, x, z, startY);
                } else {
                    // Hauteur du premier bloc plein ou liquide en partant du ciel
                    topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                }

                pos.set(x, topY, z);
                BlockState state = level.getBlockState(pos);

                int index = lz * 16 + lx;
                result.heights[index] = topY;

                if (state.isAir()) {
                    result.colors[index] = 0;
                } else {
                    MapColor mapColor = state.getMapColor(level, pos);
                    result.colors[index] = (mapColor.col == 0) ? 0 : (0xFF000000 | mapColor.col);
                }
            }
        }
        return result;
    }

    // Descend depuis startY jusqu'au premier bloc plein ou liquide
    private int findSurfaceBelow(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z, int startY) {
        int minY = level.getMinY();
        for (int y = startY; y > minY; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return minY;
    }
}