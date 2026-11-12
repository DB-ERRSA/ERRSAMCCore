package com.errsa.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

record ItemFingerprint(Material material, Map<String, Object> serializedWithoutAmount) {
    static ItemFingerprint of(ItemStack stack) {
        Map<String, Object> map = new HashMap<>(stack.serialize());
        map.remove("amount");
        return new ItemFingerprint(stack.getType(), Collections.unmodifiableMap(map));
    }
}
