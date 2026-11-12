package com.errsa.webbridge.quickshop;

import static com.errsa.webbridge.quickshop.QuickShopReflection.*;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

/** Reads live QuickShop state without requiring a compile-time QuickShop dependency. */
final class QuickShopStockReader {
    private static final int[][] INVENTORY_OFFSETS = {
            {0, 0, 0}, {0, -1, 0}, {0, 1, 0},
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {0, -2, 0}, {0, 2, 0}, {1, -1, 0},
            {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}
    };

    Collection<?> allShops() throws Exception {
        Class<?> apiType = Class.forName("com.ghostchu.quickshop.api.QuickShopAPI");
        Object api = apiType.getMethod("getInstance").invoke(null);
        Object manager = invokeNoArg(api, "getShopManager");
        if (manager == null) {
            throw new IllegalStateException("QuickShopAPI#getShopManager not found");
        }
        Object shops = invokeNoArg(manager, "getAllShops");
        if (shops instanceof Collection<?> collection) {
            return collection;
        }
        throw new IllegalStateException("ShopManager#getAllShops is not a collection");
    }

    ShopLocation location(Object shop) throws Exception {
        Object rawLocation = first(shop, "getLocation", "getShopLocation");
        if (rawLocation == null) {
            return null;
        }
        String world = worldName(rawLocation);
        Integer x = intCall(rawLocation, "getBlockX");
        Integer y = intCall(rawLocation, "getBlockY");
        Integer z = intCall(rawLocation, "getBlockZ");
        if (world == null || x == null || y == null || z == null) {
            return null;
        }
        return new ShopLocation(rawLocation, world, x, y, z);
    }

    boolean isUnlimited(Object shop) {
        return boolCall(shop, "isUnlimited", false);
    }

    Long physicalStock(Object shop, ShopLocation location) {
        try {
            Object world = invokeNoArg(location.rawLocation(), "getWorld");
            if (world == null) {
                return null;
            }
            Boolean chunkLoaded = boolCallNullable(
                    world, "isChunkLoaded", location.x() >> 4, location.z() >> 4);
            if (Boolean.FALSE.equals(chunkLoaded)) {
                return null;
            }
            Long emptyInventory = null;
            for (int[] offset : INVENTORY_OFFSETS) {
                Object block = invoke(world, "getBlockAt",
                        location.x() + offset[0],
                        location.y() + offset[1],
                        location.z() + offset[2]);
                if (block == null) {
                    continue;
                }
                Object state = invokeNoArg(block, "getState");
                Object inventory = state == null ? null : invokeNoArg(state, "getInventory");
                if (inventory == null) {
                    continue;
                }
                Object contents = invokeNoArg(inventory, "getContents");
                if (contents == null || !contents.getClass().isArray()) {
                    continue;
                }
                long stock = 0L;
                for (int index = 0; index < Array.getLength(contents); index++) {
                    Object item = Array.get(contents, index);
                    if (item == null) {
                        continue;
                    }
                    Integer count = intCall(item, "getAmount");
                    if (count != null && count > 0 && matches(shop, item)) {
                        stock += count;
                    }
                }
                if (stock > 0L) {
                    return stock;
                }
                emptyInventory = 0L;
            }
            return emptyInventory;
        } catch (Exception error) {
            return null;
        }
    }

    Long reportedStock(Object shop) {
        try {
            Method noArg = null;
            Method withBoolean = null;
            for (Method candidate : shop.getClass().getMethods()) {
                if (!candidate.getName().equals("getRemainingStock")) {
                    continue;
                }
                if (candidate.getParameterCount() == 0) {
                    noArg = candidate;
                } else if (candidate.getParameterCount() == 1) {
                    Class<?> parameter = candidate.getParameterTypes()[0];
                    if (parameter == boolean.class || parameter == Boolean.class) {
                        withBoolean = candidate;
                    }
                }
            }
            Object stock = withBoolean != null ? withBoolean.invoke(shop, true)
                    : noArg != null ? noArg.invoke(shop) : null;
            return numberFrom(stock);
        } catch (Exception error) {
            return null;
        }
    }

    private boolean matches(Object shop, Object item) {
        try {
            for (Method method : shop.getClass().getMethods()) {
                if (method.getName().equals("matches") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(item.getClass())) {
                    Object result = method.invoke(shop, item);
                    if (result instanceof Boolean match) {
                        return match;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to ItemStack similarity and material identity.
        }
        try {
            Object shopItem = first(shop, "getItem", "getItemStack", "getShopItem");
            if (shopItem == null) {
                return false;
            }
            for (Method method : shopItem.getClass().getMethods()) {
                if (method.getName().equals("isSimilar") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(item.getClass())) {
                    return Boolean.TRUE.equals(method.invoke(shopItem, item));
                }
            }
            String expected = materialName(shopItem);
            return expected != null && expected.equals(materialName(item));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String materialName(Object item) throws Exception {
        if (item == null) {
            return null;
        }
        Object type = invokeNoArg(item, "getType");
        if (type != null) {
            return String.valueOf(type);
        }
        Object nested = first(item, "getItemStack", "asItemStack", "getItem");
        return nested != null && nested != item ? materialName(nested) : null;
    }
}

record ShopLocation(Object rawLocation, String world, int x, int y, int z) {
    String key() {
        return world + "|" + x + "|" + y + "|" + z;
    }
}
