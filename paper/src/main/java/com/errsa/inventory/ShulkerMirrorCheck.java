package com.errsa.inventory;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Checks for a complete shulker payload mirrored in loose player inventory. */
final class ShulkerMirrorCheck {
    private ShulkerMirrorCheck() {
    }

    static Result check(Inventory shulkerInventory,
                        PlayerInventory playerInventory,
                        Settings settings) {
        Map<ItemFingerprint, Integer> shulker = itemCounts(
                shulkerInventory.getContents(), false);
        if (shulker.isEmpty()) {
            return null;
        }
        Map<ItemFingerprint, Integer> loose = itemCounts(
                playerInventory.getStorageContents(), true);

        int overlap = 0;
        int total = 0;
        int mirroredDistinct = 0;
        boolean fullyMirrored = true;
        for (Map.Entry<ItemFingerprint, Integer> entry : shulker.entrySet()) {
            int looseCount = loose.getOrDefault(entry.getKey(), 0);
            total += entry.getValue();
            overlap += Math.min(entry.getValue(), looseCount);
            if (looseCount >= entry.getValue()) {
                mirroredDistinct++;
            } else {
                fullyMirrored = false;
            }
        }

        boolean strongMirror = fullyMirrored
                && total >= settings.minimumShulkerOverlapItems()
                && (mirroredDistinct >= 2
                || total >= settings.minimumSingleFingerprintMirrorItems());
        if (!strongMirror) {
            return null;
        }
        return new Result(overlap, mirroredDistinct,
                countOccupied(shulkerInventory.getContents()));
    }

    private static Map<ItemFingerprint, Integer> itemCounts(
            ItemStack[] items, boolean ignoreShulkerItems) {
        Map<ItemFingerprint, Integer> counts = new HashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Material material = item.getType();
            if (ignoreShulkerItems && material.name().endsWith("SHULKER_BOX")) {
                continue;
            }
            counts.merge(ItemFingerprint.of(item), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    private static int countOccupied(ItemStack[] contents) {
        int occupied = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                occupied++;
            }
        }
        return occupied;
    }

    record Result(int mirroredItems, int distinctItems, int shulkerSlots) {
        String description() {
            return "mirroredItems=" + mirroredItems
                    + " distinct=" + distinctItems
                    + " shulkerSlots=" + shulkerSlots
                    + " source=SHULKER_CLOSE";
        }
    }
}
