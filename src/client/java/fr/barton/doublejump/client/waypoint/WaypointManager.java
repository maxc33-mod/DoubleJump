package fr.barton.doublejump.client.waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WaypointManager {

    private String key = null; // fichier des waypoints actuels
    private final List<Waypoint> waypoints = new ArrayList<>();

    // À appeler avant d'utiliser les waypoints : recharge la liste si on a changé de monde/dimension
    public void useKey(String newKey) {
        if (Objects.equals(key, newKey)) {
            return;
        }
        key = newKey;
        waypoints.clear();
        if (key != null) {
            waypoints.addAll(WaypointStorage.load(key));
        }
    }

    public List<Waypoint> all() {
        return waypoints;
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public void remove(Waypoint waypoint) {
        waypoints.remove(waypoint);
        save();
    }

    // À appeler après avoir modifié un waypoint existant
    public void save() {
        if (key != null) {
            WaypointStorage.save(key, waypoints);
        }
    }
}