package me.RedEagle3.eRRSAMCCore.GUI;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuGUI implements Listener {

    private final ERRSAMCCore plugin;

    private static final String MENU_TITLE = "Server Menu";

    public MenuGUI(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory menu = Bukkit.createInventory(null, 36, MENU_TITLE);

        // Discord
        ItemStack discord = createItem(
                Material.PAPER,
                ChatColor.GOLD + "Discord",
                ChatColor.GRAY + "Join the ERRSA Discord server."
        );
        menu.setItem(10, discord);


        // Website
        ItemStack website = createItem(
                Material.KNOWLEDGE_BOOK,
                ChatColor.GOLD + "Website",
                ChatColor.GRAY + "Visit the ERRSA website."
        );
        menu.setItem(11, website);



        // World Map
        ItemStack map = createItem(
                Material.FILLED_MAP,
                ChatColor.GOLD + "World Map",
                ChatColor.GRAY + "View the live online world map."
        );
        menu.setItem(12, map);


        // Warps
        ItemStack warps = createItem(
                Material.ENDER_PEARL,
                ChatColor.GOLD + "Warps",
                ChatColor.GRAY + "View available server warps."
        );
        menu.setItem(13, warps);


        // Guilds
        ItemStack guilds = createItem(
                Material.SHIELD,
                ChatColor.GOLD + "Guilds",
                ChatColor.GRAY + "Create and manage your guild."
        );
        menu.setItem(14, guilds);


        // Quests
        ItemStack quests = createItem(
                Material.WRITABLE_BOOK,
                ChatColor.GOLD + "Quests",
                ChatColor.GRAY + "View your available quests."
        );
        menu.setItem(15, quests);


        // Help
        ItemStack help = createItem(
                Material.KNOWLEDGE_BOOK,
                ChatColor.GOLD + "Help",
                ChatColor.GRAY + "Need help? View useful information."
        );
        menu.setItem(16, help);


        // Progression
        ItemStack progression = createItem(
                Material.EXPERIENCE_BOTTLE,
                ChatColor.GOLD + "Progression",
                ChatColor.GRAY + "View your player progression."
        );
        menu.setItem(19, progression);


        // Leaderboard
        ItemStack leaderboard = createItem(
                Material.GOLD_INGOT,
                ChatColor.GOLD + "Leaderboard",
                ChatColor.GRAY + "View the server leaderboards."
        );
        menu.setItem(20, leaderboard);


        // Server Browser
        ItemStack serverBrowser = createItem(
                Material.COMPASS,
                ChatColor.GOLD + "Server Browser",
                ChatColor.GRAY + "Browse available servers."
        );
        menu.setItem(21, serverBrowser);


        // Token Shop
        ItemStack tokenShop = createItem(
                Material.EMERALD,
                ChatColor.GOLD + "Token Shop",
                ChatColor.GRAY + "Spend your tokens on rewards."
        );
        menu.setItem(22, tokenShop);


        // Mailbox
        ItemStack mailbox = createItem(
                Material.CHEST,
                ChatColor.GOLD + "Mailbox",
                ChatColor.GRAY + "View your messages and rewards."
        );
        menu.setItem(23, mailbox);


        player.openInventory(menu);
    }


    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().getTitle().equals(MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }


        // Discord
        if (event.getSlot() == 10) {
            player.closeInventory();
            player.performCommand("discord");
        }


        // Website
        else if (event.getSlot() == 11) {
            player.closeInventory();
            player.performCommand("website");
        }


        // World map
        else if (event.getSlot() == 12) {
            player.closeInventory();
            player.performCommand("map");
        }


        // Warps
        else if (event.getSlot() == 13) {
            player.closeInventory();
            player.performCommand("warps");
        }


        // Guilds
        else if (event.getSlot() == 14) {
            player.closeInventory();
            player.performCommand("guild");
        }


        // Quests
        else if (event.getSlot() == 15) {
            player.closeInventory();
            player.performCommand("quests");
        }


        // Help
        else if (event.getSlot() == 16) {
            player.closeInventory();
            player.performCommand("tutorial");
        }


        // Progression
        else if (event.getSlot() == 19) {
            player.closeInventory();
            player.performCommand("progression");
        }


        // Leaderboard
        else if (event.getSlot() == 20) {
            player.closeInventory();
            player.performCommand("playtime top");
        }


        // Server browser
        else if (event.getSlot() == 21) {
            player.closeInventory();
            player.performCommand("servers");
        }


        // Token Shop
        else if (event.getSlot() == 22) {
            player.closeInventory();
            player.performCommand("tokens");
        }


        // Mailbox
        else if (event.getSlot() == 23) {
            player.closeInventory();
            player.performCommand("mailbox");
        }
    }


    private ItemStack createItem(Material material, String name, String description) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            if (description != null) {
                meta.setLore(Arrays.asList(description));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}