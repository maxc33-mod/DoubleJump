package fr.barton.doublejump.client.map;

import fr.barton.doublejump.DoubleJump;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.loader.api.FabricLoader;

public class MapStorage {

    private static final int FORMAT_VERSION = 1;

    // Un seul "ouvrier" en arrière-plan : les sauvegardes et chargements passent
    // les uns après les autres, dans l'ordre.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "doublejump-map-io");
        thread.setDaemon(true);
        return thread;
    });

    private static Path fileFor(String key) {
        return FabricLoader.getInstance().getGameDir()
                .resolve("doublejump").resolve("maps").resolve(key + ".dat");
    }

    // Sauvegarde en arrière-plan (le jeu ne ralentit pas)
    public static void saveAsync(String key, Map<Long, MapChunkData> snapshot) {
        try {
            IO.submit(() -> write(key, snapshot));
        } catch (RejectedExecutionException e) {
            // le jeu est en train de se fermer, on ne peut plus rien planifier
        }
    }

    // Chargement : attend d'abord la fin des sauvegardes en cours,
    // pour ne jamais lire un fichier à moitié écrit.
    public static Map<Long, MapChunkData> load(String key) {
        try {
            return IO.submit(() -> read(key)).get();
        } catch (Exception e) {
            DoubleJump.LOGGER.error("Chargement de la carte impossible : {}", key, e);
            return new HashMap<>();
        }
    }

    // À la fermeture du jeu : on laisse finir les sauvegardes en attente
    public static void finishAndStop() {
        IO.shutdown();
        try {
            IO.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void write(String key, Map<Long, MapChunkData> snapshot) {
        Path target = fileFor(key);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            Files.createDirectories(target.getParent());

            // On écrit d'abord dans un fichier temporaire, compressé
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(temp))))) {

                out.writeInt(FORMAT_VERSION);
                out.writeInt(snapshot.size());

                for (Map.Entry<Long, MapChunkData> entry : snapshot.entrySet()) {
                    long packed = entry.getKey();
                    out.writeInt((int) (packed >> 32)); // coordonnée X du chunk
                    out.writeInt((int) packed);         // coordonnée Z du chunk

                    MapChunkData chunk = entry.getValue();
                    for (int i = 0; i < 256; i++) {
                        out.writeInt(chunk.colors[i]);
                    }
                    for (int i = 0; i < 256; i++) {
                        out.writeShort(chunk.heights[i]);
                    }
                }
            }

            // Puis on remplace l'ancien fichier d'un seul coup
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }

            DoubleJump.LOGGER.debug("Carte sauvegardée : {} ({} chunks)", key, snapshot.size());
        } catch (IOException e) {
            DoubleJump.LOGGER.error("Sauvegarde de la carte impossible : {}", key, e);
        }
    }

    private static Map<Long, MapChunkData> read(String key) {
        Map<Long, MapChunkData> result = new HashMap<>();
        Path file = fileFor(key);
        if (!Files.exists(file)) {
            return result; // premier passage dans ce monde : rien à charger
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))) {

            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                DoubleJump.LOGGER.warn("Format de carte inconnu ({}) pour {}", version, key);
                return result;
            }

            int count = in.readInt();
            for (int n = 0; n < count; n++) {
                int cx = in.readInt();
                int cz = in.readInt();

                MapChunkData chunk = new MapChunkData();
                for (int i = 0; i < 256; i++) {
                    chunk.colors[i] = in.readInt();
                }
                for (int i = 0; i < 256; i++) {
                    chunk.heights[i] = in.readShort();
                }
                result.put(WorldMapData.key(cx, cz), chunk);
            }
        } catch (IOException e) {
            DoubleJump.LOGGER.error("Lecture de la carte impossible : {}", key, e);
        }
        return result;
    }
}