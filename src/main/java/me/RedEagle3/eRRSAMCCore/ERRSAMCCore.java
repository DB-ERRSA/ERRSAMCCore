package me.RedEagle3.eRRSAMCCore;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class ERRSAMCCore extends JavaPlugin implements TabExecutor {

    private Economy economy;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("sell").setExecutor(this::onSellCommand);
        getCommand("discord").setExecutor(this::onDiscordCommand);
        getCommand("feedback").setExecutor(this::onFeedbackCommand);
        getCommand("report").setExecutor(this::onReportCommand);
        getCommand("tutorial").setExecutor(this::onTutorialCommand);
        getCommand("resetschedule").setExecutor(this::onResetScheduleCommand);
        this.getCommand("sell").setTabCompleter(this);

        saveDefaultConfig();
        getLogger().info("ERRSA MC Core has been enabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("sell")) {
            if (args.length == 1) {
                List<String> suggestions = List.of("1", "5", "all");

                // Filter suggestions by what the player already typed
                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private boolean onSellCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        int diamondsToSell = 1;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("all")) {
                diamondsToSell = countDiamonds(player);
            } else {
                try {
                    diamondsToSell = Integer.parseInt(args[0]);
                    if (diamondsToSell <= 0) {
                        player.sendMessage(ChatColor.RED + "Please enter a positive number.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount. Use /sell <number> or /sell all.");
                    return true;
                }
            }
        }

        int available = countDiamonds(player);
        if (available == 0) {
            // player.sendMessage(ChatColor.RED + "You don't have any diamonds to sell.");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "You don't have any diamonds to sell."));

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        } else if (available < diamondsToSell) {
            // player.sendMessage(ChatColor.RED + "You only have " + available + " diamonds available.");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "You only have " + available + " diamonds available."));

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        removeDiamonds(player, diamondsToSell);

        double pricePer = getConfig().getDouble("sell.diamond-price", 50.0);
        double total = diamondsToSell * pricePer;
        economy.depositPlayer(player, total);

        // player.sendMessage(ChatColor.GREEN + "Sold " + diamondsToSell + " diamonds for $" + total);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GREEN + "Sold " + diamondsToSell + " diamonds for " + ChatColor.GOLD + "$" + total));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 1, 1, 1);

        return true;
    }

    // Supporting function for onSellCommand
    private int countDiamonds(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                total += item.getAmount();
            }
        }
        return total;
    }

    // Supporting function for onSellCommand
    private void removeDiamonds(Player player, int amount) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.DIAMOND) {
                int stackAmount = item.getAmount();
                if (stackAmount > amount) {
                    item.setAmount(stackAmount - amount);
                    break;
                } else {
                    inv.setItem(i, null);
                    amount -= stackAmount;
                    if (amount <= 0) break;
                }
            }
        }
    }

    private boolean onDiscordCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = getConfig().getString("discord-link", "https://discord.com/invite/XvKYDe3pGX");

        if (!isPlayerOnBedrock) {
            TextComponent linkText = new TextComponent("Click here to join!");
            linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to join our Discord!").color(net.md_5.bungee.api.ChatColor.GOLD).create()));

            player.spigot().sendMessage(new ComponentBuilder("[Discord] ").color(net.md_5.bungee.api.ChatColor.GOLD).append(linkText).create());
        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "[Discord] " + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please type "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + net.md_5.bungee.api.ChatColor.UNDERLINE + link + net.md_5.bungee.api.ChatColor.AQUA + " into any internet browser.");
        }
        return true;
    }

    private boolean onFeedbackCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = getConfig().getString("feedback-link", "https://campusgroups.erau.edu/errsa/craft/");

        if (!isPlayerOnBedrock) {
            TextComponent linkText = new TextComponent("Click here for our feedback form!");
            linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("We would love to hear your ideas!")
                            .color(net.md_5.bungee.api.ChatColor.GOLD)
                            .create()));

            player.spigot().sendMessage(new ComponentBuilder("[Feedback] ")
                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                    .append(linkText).create());
        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "[Feedback] "
                    + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please enter "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + "" + net.md_5.bungee.api.ChatColor.UNDERLINE + link
                    + net.md_5.bungee.api.ChatColor.AQUA + " into your browser, "
                    + "or check the Minecraft Welcome channel in our Discord for the direct link.");
        }
        return true;
    }

    private boolean onReportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = getConfig().getString("report-link", "https://campusgroups.erau.edu/errsa/craft/");

        if (!isPlayerOnBedrock) {
            TextComponent linkText = new TextComponent("Click here for the report form.");
            linkText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to be taken to the report form.")
                            .color(net.md_5.bungee.api.ChatColor.DARK_RED)
                            .create()));

            player.spigot().sendMessage(new ComponentBuilder("[Report] ")
                    .color(net.md_5.bungee.api.ChatColor.DARK_RED)
                    .append(linkText).create());
        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.DARK_RED + "[Report] "
                    + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please enter "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + "" + net.md_5.bungee.api.ChatColor.UNDERLINE + link
                    + net.md_5.bungee.api.ChatColor.AQUA + " into your browser, "
                    + "or check the Minecraft Welcome channel in our Discord for the direct link.");
        }
        return true;
    }

    private boolean onTutorialCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // Make the player run /warp tutorial
        player.performCommand("warp tutorial");

        return true;
    }

    private boolean onResetScheduleCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only available to players.");
            return true;
        }

        Player player = (Player) sender;

        String date = getConfig().getString("reset-schedule.reset_date", "TBD");
        String cycle = getConfig().getString("reset-schedule.reset_cycle", "few");

        player.sendMessage(ChatColor.GOLD + "The next server reset is planned for " + ChatColor.AQUA + date + ChatColor.GOLD + ".");
        player.sendMessage(ChatColor.GRAY + "This typically takes place every " + ChatColor.DARK_AQUA + cycle + ChatColor.GRAY + " years to keep the server fresh and updated.");

        return true;
    }
}