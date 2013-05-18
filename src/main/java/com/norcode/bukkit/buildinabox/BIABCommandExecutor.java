package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BIABCommandExecutor implements CommandExecutor {
    BuildInABox plugin;
    public BIABCommandExecutor(BuildInABox buildInABox) {
        this.plugin = buildInABox;
    }

    public boolean onCommand(CommandSender sender, Command command, String alias, String[] params) {
        LinkedList<String> args = new LinkedList<String>(Arrays.asList(params));
        if (args.size() == 0) {
            return false;
        }
        String action = args.pop().toLowerCase();
        if (action.equals("give")) {
            cmdGive(sender, args);
            return true;
        } else if (action.equals("save")) {
            cmdSave(sender, args);
            return true;
        } else if (action.equals("list")) {
            cmdList(sender, args);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unexpected argument: " + action);
        return true;
    }
    private void cmdList(CommandSender sender, LinkedList<String> args) {
        int page = 1;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.peek());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Invalid Page: " + args.peek());
                return;
            }
        }
        int numPages = (int) Math.ceil(plugin.getDataStore().getAllBuildingPlans().size() / 8.0f);
        if (numPages == 0) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GRAY + "There are no building plans saved yet.");
            return;
        }
        List<BuildingPlan> plans = new ArrayList<BuildingPlan>(plugin.getDataStore().getAllBuildingPlans());
        List<String> lines = new ArrayList<String>();
        lines.add(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.WHITE + "Available Building Plans [pg." + page + " of " + numPages + "]");
        for (int i=8*(page-1);i<8*(page);i++) {
            if (i<plans.size()) {
                lines.add(ChatColor.GOLD + " * " + ChatColor.GRAY + plans.get(i).getName());
            }
        }
        sender.sendMessage(lines.toArray(new String[lines.size()]));
    }

    public void cmdSave(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            return;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "This command cannot be run from the console.");
            return;
        }
        String buildingName = args.pop();
        BuildingPlan plan = BuildingPlan.fromClipboard(plugin, (Player) sender, buildingName);
        if (plan != null) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GREEN + "Building plan " + ChatColor.WHITE + plan.getName() + ChatColor.GREEN + " saved!");
            plugin.getDataStore().saveBuildingPlan(plan);
        }
    }

    public void cmdGive(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.give")) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            return;
        }
        Player targetPlayer = null;
        if (args.size() >= 2) {
            String name = args.pop();
            List<Player> matches = plugin.getServer().matchPlayer(name);
            if (matches.size() == 1) {
                targetPlayer = matches.get(0);
            } else {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unknown Player: " + name);
                return;
            }
        }
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Cannot give a building to the console.");
                return;
            } else {
                targetPlayer = (Player) sender;
            }
        }
        if (args.size() == 1) {
            String planName = args.pop().toLowerCase();
            BuildingPlan plan = plugin.getDataStore().getBuildingPlan(planName);
            if (plan == null) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unknown Building Plan: " + planName + ".");
                return;
            }
            ChestData data = plugin.getDataStore().createChest(plan.getName());
            ItemStack stack = data.toItemStack();
            if (targetPlayer.getInventory().addItem(stack).size() > 0) {
                targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), stack);
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.WHITE + "Usage: /biab give [<player>] <plan>");
        }
    }
}
