package com.errsa.inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Fingerprints a player's complete inventory including item metadata. */
final class AuditInventorySnapshot {
    private AuditInventorySnapshot() {
    }

    static int hash(PlayerInventory inventory) {
        int result = 1;
        for (ItemStack item : inventory.getContents()) {
            int itemHash = item == null || item.getType().isAir()
                    ? 0 : item.serialize().hashCode();
            result = 31 * result + itemHash;
        }
        return result;
    }
}
