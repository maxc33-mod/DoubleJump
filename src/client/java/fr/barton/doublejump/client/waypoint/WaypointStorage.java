package fr.barton.doublejump.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import fr.barton.doublejump.DoubleJump;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public class WaypointStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {}.getType();

    // Même dossier que les cartes : doublejump/maps/<monde>/<dimension>.waypoints.json
    private static Path fileFor(String key) {
        return FabricLoader.getInstance().getGameDir()
                .resolve("doublejump").resolve("maps").resolve(key + ".waypoints.json");
    }

    public static List<Waypoint> load(String key) {
        List<Waypoint> result = new ArrayList<>();
        Path file = fileFor(key);
        if (!Files.exists(file)) {
            return result; // aucun waypoint enregistré ici
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            List<Waypoint> loaded = GSON.fromJson(json, LIST_TYPE);
            if (loaded != null) {
                for (Waypoint waypoint : loaded) {
                    if (waypoint.name == null) {
                        waypoint.name = "";
                    }
                    result.add(waypoint);
                }
            }
        } catch (IOException | JsonParseException e) {
            DoubleJump.LOGGER.error("Lecture des waypoints impossible : {}", key, e);
        }
        return result;
    }

    public static void save(String key, List<Waypoint> waypoints) {
        Path target = fileFor(key);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(temp, GSON.toJson(waypoints, LIST_TYPE), StandardCharsets.UTF_8);

            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            DoubleJump.LOGGER.error("Sauvegarde des waypoints impossible : {}", key, e);
        }
    }
}