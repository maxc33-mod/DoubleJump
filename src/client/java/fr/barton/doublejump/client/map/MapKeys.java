package fr.barton.doublejump.client.map;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public class MapKeys {

    private static final String ID_FILE = "doublejump_world_id.txt";

    // Exemple de résultat : "solo_3f2a...-uuid/overworld"
    public static String keyFor(Minecraft client, ClientLevel level) {
        return worldName(client) + "/" + dimensionName(level);
    }

    private static String worldName(Minecraft client) {
        // Solo : identifiant unique stocké dans le dossier du monde
        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();

            String id = soloWorldId(root);
            if (id != null) {
                return "solo_" + id;
            }

            // Secours si on ne peut pas écrire dans le dossier : le nom du dossier
            Path folder = root.getFileName();
            return "solo_" + safe(folder != null ? folder.toString() : "unknown");
        }

        // Multijoueur : on utilise l'adresse du serveur
        ServerData serverData = client.getCurrentServer();
        if (serverData != null) {
            return "server_" + safe(serverData.ip);
        }

        return "unknown";
    }

    // Lit l'identifiant du monde, ou le crée la première fois
    private static String soloWorldId(Path worldRoot) {
        Path idFile = worldRoot.resolve(ID_FILE);
        try {
            if (Files.exists(idFile)) {
                try {
                    return UUID.fromString(Files.readString(idFile, StandardCharsets.UTF_8).trim()).toString();
                } catch (IllegalArgumentException e) {
                    // fichier invalide : on en recrée un juste après
                }
            }
            String id = UUID.randomUUID().toString();
            Files.writeString(idFile, id, StandardCharsets.UTF_8);
            return id;
        } catch (IOException e) {
            return null;
        }
    }

    private static String dimensionName(ClientLevel level) {
        if (Level.OVERWORLD.equals(level.dimension())) {
            return "overworld";
        }
        if (Level.NETHER.equals(level.dimension())) {
            return "nether";
        }
        if (Level.END.equals(level.dimension())) {
            return "the_end";
        }
        // Dimension ajoutée par un autre mod
        return "other_" + safe(level.dimension().identifier().toString());
    }

    // Transforme un texte en nom de fichier valide (sans caractères interdits).
    // Le code ajouté à la fin évite que deux noms différents donnent le même résultat.
    private static String safe(String text) {
        String cleaned = text.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60);
        }
        return cleaned + "-" + Integer.toHexString(text.hashCode());
    }
}