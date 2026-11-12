package org.errsa.combatdiag;

import java.util.UUID;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Observe attack packets without changing or cancelling them. */
final class PacketBridge {

    private final CombatDiagnosticsModule diagnostics;
    private ProtocolManager manager;
    private PacketAdapter adapter;
    private boolean active;
    private String state = "not started";

    PacketBridge(CombatDiagnosticsModule diagnostics) {
        this.diagnostics = diagnostics;
    }

    boolean isActive() {
        return active;
    }

    String status() {
        return state;
    }

    void start() {
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            state = "ProtocolLib missing (Bukkit-only capture)";
            return;
        }

        try {
            manager = ProtocolLibrary.getProtocolManager();
            adapter = new PacketAdapter(diagnostics.getPlugin(),
                    ListenerPriority.MONITOR, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    observeAttack(event);
                }
            };
            manager.addPacketListener(adapter);
            active = true;
            state = "ProtocolLib ATTACK observation active (not lag compensated)";
        } catch (LinkageError | RuntimeException ex) {
            state = "ProtocolLib incompatible: " + ex.getClass().getSimpleName();
            diagnostics.packetBridgeWarning(state);
        }
    }

    private void observeAttack(PacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        long receivedAt = System.currentTimeMillis();
        try {
            var actions = event.getPacket().getEnumEntityUseActions();
            if (actions.size() == 0 || actions.read(0) == null
                    || !"ATTACK".equals(actions.read(0).getAction().name())) {
                return;
            }

            UUID attackerId = event.getPlayer().getUniqueId();
            int targetId = event.getPacket().getIntegers().read(0);
            // World and entity lookups must stay on the primary thread.
            Bukkit.getScheduler().runTask(diagnostics.getPlugin(),
                    () -> resolveAttack(attackerId, targetId, receivedAt));
        } catch (RuntimeException ex) {
            if (active) {
                active = false;
                diagnostics.packetBridgeWarning("Packet action accessor failed: "
                        + ex.getClass().getSimpleName());
                Bukkit.getScheduler().runTask(diagnostics.getPlugin(), this::stop);
            }
        }
    }

    private void resolveAttack(UUID attackerId, int targetId, long receivedAt) {
        Player attacker = Bukkit.getPlayer(attackerId);
        if (attacker == null) {
            return;
        }

        Entity target = null;
        for (Entity candidate : attacker.getNearbyEntities(16, 16, 16)) {
            if (candidate.getEntityId() == targetId) {
                target = candidate;
                break;
            }
        }

        Player targetPlayer = target instanceof Player player ? player : null;
        diagnostics.packetAttack(attacker, targetPlayer, receivedAt);
    }

    void stop() {
        if (manager != null && adapter != null) {
            manager.removePacketListener(adapter);
        }
        manager = null;
        adapter = null;
        active = false;
        state = "stopped";
    }
}
