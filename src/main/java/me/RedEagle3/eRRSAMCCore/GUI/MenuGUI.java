package me.RedEagle3.eRRSAMCCore.GUI;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MenuGUI implements Listener {

    private final ERRSAMCCore plugin;

    private static final String MENU_TITLE = "Server Menu";

    public MenuGUI(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE);

        // Server Browser
        ItemStack serverBrowser = createItem(
                Material.COMPASS,
                ChatColor.GOLD + "Server Browser",
                ChatColor.GRAY + "Browse available servers and events."
        );
        menu.setItem(10, serverBrowser);


        // Warps
        ItemStack warps = createItem(
                Material.ENDER_PEARL,
                ChatColor.GOLD + "Warps",
                ChatColor.GRAY + "View available server warps."
        );
        menu.setItem(12, warps);


        // Guilds
        ItemStack guilds = createItem(
                Material.SHIELD,
                ChatColor.GOLD + "Guilds",
                ChatColor.GRAY + "Create, join, or manage your guild."
        );
        menu.setItem(14, guilds);


        // Quests
        ItemStack quests = createItem(
                Material.WRITABLE_BOOK,
                ChatColor.GOLD + "Quests",
                ChatColor.GRAY + "View available server quests."
        );
        menu.setItem(16, quests);


        // Progression
        ItemStack progression = createItem(
                Material.EXPERIENCE_BOTTLE,
                ChatColor.GOLD + "Progression",
                ChatColor.GRAY + "View your player progression."
        );
        menu.setItem(28, progression);


        // Leaderboard
        ItemStack leaderboard = createItem(
                Material.GOLD_INGOT,
                ChatColor.GOLD + "Leaderboard",
                ChatColor.GRAY + "View the server rank leaderboard."
        );
        menu.setItem(30, leaderboard);


        // Token Shop
        ItemStack tokenShop = createItem(
                Material.EMERALD,
                ChatColor.GOLD + "Token Shop",
                ChatColor.GRAY + "View, use, and buy custom item tokens."
        );
        menu.setItem(32, tokenShop);


        // Mailbox
        ItemStack mailbox = createItem(
                Material.CHEST,
                ChatColor.GOLD + "Mailbox",
                ChatColor.GRAY + "View server messages and announcements."
        );
        menu.setItem(34, mailbox);



        // Website
        ItemStack website = createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzYyNGY1NDNiMTdhNzFkZmYxZmJmYzVhYWJjYjAyZTRlNjVhYTJjMWY1MDVjN2JkMWQ3YWRmNjY3ZGI2ODliOGYifX19",
                ChatColor.GOLD + "Website",
                ChatColor.GRAY + "Visit the ERRSA website."
        );
        menu.setItem(48, website);


        // Discord
        ItemStack discord = createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EzYjE4M2IxNDhiOWI0ZTJiMTU4MzM0YWZmM2I1YmI2YzJjMmRiYmM0ZDY3Zjc2YTdiZTg1NjY4N2EyYjYyMyJ9fX0=",
                ChatColor.GOLD + "Discord",
                ChatColor.GRAY + "Join the ERRSA Discord server."
        );
        menu.setItem(49, discord);


        // World Map
        ItemStack map = createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlMmNjNDIwMTVlNjY3OGY4ZmQ0OWNjYzAxZmJmNzg3ZjFiYTJjMzJiY2Y1NTlhMDE1MzMyZmM1ZGI1MCJ9fX0=",
                ChatColor.GOLD + "World Map",
                ChatColor.GRAY + "View the live online world map."
        );
        menu.setItem(50, map);



        // Help
        ItemStack help = createItem(
                Material.KNOWLEDGE_BOOK,
                ChatColor.GOLD + "Need Help?",
                Arrays.asList(ChatColor.GRAY + "Visit the " + ChatColor.AQUA + "/tutorial" + ChatColor.GRAY + " area for in-game tutorials.",
                                "",
                                ChatColor.GRAY + "Join our " + ChatColor.AQUA + "Discord" + ChatColor.GRAY + " to ask staff for help.",
                                "",
                                ChatColor.GRAY + "Click here to visit our " + ChatColor.AQUA + "website's handbook" + ChatColor.GRAY + " for online guides."));
        menu.setItem(53, help);


        player.openInventory(menu);
    }


    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {return;}
        if (!event.getView().getTitle().equals(MENU_TITLE)) {return;}
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {return;}
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {return;}

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);


        // Server browser
        if (event.getSlot() == 10) {
            player.closeInventory();
            player.performCommand("servers");
        }


        // Warps
        else if (event.getSlot() == 12) {
            player.closeInventory();
            player.performCommand("warps");
        }


        // Guilds
        else if (event.getSlot() == 14) {
            player.closeInventory();
            player.performCommand("guild");
        }


        // Quests
        else if (event.getSlot() == 16) {
            player.closeInventory();
            player.performCommand("quests");
        }


        // Progression
        else if (event.getSlot() == 28) {
            player.closeInventory();
            player.performCommand("progression");
        }


        // Leaderboard
        else if (event.getSlot() == 30) {
            player.closeInventory();
            player.performCommand("playtime top");
        }


        // Token Shop
        else if (event.getSlot() == 32) {
            player.closeInventory();
            player.performCommand("tokens");
        }


        // Mailbox
        else if (event.getSlot() == 34) {
            player.closeInventory();
            player.performCommand("mailbox");
        }


        // Website
        else if (event.getSlot() == 48) {
            player.closeInventory();
            player.performCommand("website");
        }


        // Discord
        else if (event.getSlot() == 49) {
            player.closeInventory();
            player.performCommand("discord");
        }


        // World map
        else if (event.getSlot() == 50) {
            player.closeInventory();
            player.performCommand("map");
        }


        // Help
        else if (event.getSlot() == 53) {

            String link = "https://errsa-minecraft-handbook.onrender.com/handbook.html";

            TextComponent prefix = new TextComponent("[Handbook] ");
            prefix.setColor(net.md_5.bungee.api.ChatColor.GOLD);

            TextComponent linkText = new TextComponent("Click here to open the Handbook!");
            linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to view the online Handbook").color(net.md_5.bungee.api.ChatColor.GOLD).create()));

            prefix.addExtra(linkText);

            player.closeInventory();
            player.spigot().sendMessage(prefix);
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

    private ItemStack createItem(Material material, String name, List<String> description) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            if (description != null) {
                meta.setLore(description);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createCustomHead(String textureValue, String name, String description) {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());

            profile.setProperty(new ProfileProperty("textures", textureValue));

            meta.setPlayerProfile(profile);

            meta.setDisplayName(name);

            if (description != null) {
                meta.setLore(Arrays.asList(description));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}