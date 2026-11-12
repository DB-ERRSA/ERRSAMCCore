package com.errsa.webbridge.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Captures Bukkit player state on the server thread. */
public final class PlayerSnapshotService {
    public Map<String, Object> capture() {
        try {
            List<Map<String, Object>> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", player.getName());
                entry.put("uuid", player.getUniqueId().toString());
                entry.put("display_name", player.getDisplayName());
                if (player.getWorld() != null) {
                    entry.put("world", player.getWorld().getName());
                }
                players.add(entry);
            }
            players.sort(Comparator.comparing(
                    entry -> String.valueOf(entry.get("name")),
                    String.CASE_INSENSITIVE_ORDER));
            Map<String, Object> snapshot = empty();
            snapshot.put("online", players.size());
            snapshot.put("max", Bukkit.getMaxPlayers());
            snapshot.put("list", players);
            return snapshot;
        } catch (Exception error) {
            Map<String, Object> unavailable = empty();
            unavailable.put("available", false);
            unavailable.put("error", error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            return unavailable;
        }
    }

    public static Map<String, Object> empty() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("available", true);
        snapshot.put("online", 0);
        snapshot.put("max", 0);
        snapshot.put("list", new ArrayList<>());
        return snapshot;
    }
}
