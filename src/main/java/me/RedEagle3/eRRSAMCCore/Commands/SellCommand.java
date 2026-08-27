package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final ERRSAMCCore plugin;
    private final Economy economy;

    public SellCommand(ERRSAMCCore plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

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

        double pricePer = plugin.getConfig().getDouble("sell.diamond-price", 50.0);
        double total = diamondsToSell * pricePer;
        economy.depositPlayer(player, total);

        // player.sendMessage(ChatColor.GREEN + "Sold " + diamondsToSell + " diamonds for $" + total);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GREEN + "Sold " + diamondsToSell + " diamonds for " + ChatColor.GOLD + "$" + total));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 1, 1, 1);

        return true;
    }

    private int countDiamonds(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                total += item.getAmount();
            }
        }
        return total;
    }

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            List<String> suggestions = List.of("1", "5", "all");

            // Filter suggestions by what the player already typed
            return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }
        return null;
    }
}
