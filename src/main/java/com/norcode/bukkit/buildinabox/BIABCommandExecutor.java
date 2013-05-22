package com.norcode.bukkit.buildinabox;

import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class BIABCommandExecutor implements TabExecutor {
    BuildInABox plugin;
    Pattern colorPattern = Pattern.compile("(&[0-9a-flmnor])", Pattern.CASE_INSENSITIVE);
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
        } else if (action.equals("delete")) {
            cmdDelete(sender, args);
            return true;
        } else if (action.toLowerCase().startsWith("setdisplayname")) {
            cmdSetDisplayName(sender, args);
            return true;
        } else if (action.toLowerCase().startsWith("setdesc")) {
            cmdSetDescription(sender, args);
            return true;
        } else if (action.toLowerCase().startsWith("permanent")) {
            cmdPermanent(sender, args);
            return true;
        }
        sender.sendMessage(BuildInABox.getErrorMsg("unexpected-argument", action));
        return true;
    }

    private void cmdPermanent(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.permanent")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(BuildInABox.getErrorMsg("cannot-use-from-console"));
            return;
        }
        sender.sendMessage(BuildInABox.getNormalMsg("punch-to-make-permanent"));
        ((Player)sender).setMetadata("biab-permanent-timeout", new FixedMetadataValue(plugin, System.currentTimeMillis()+3000));
        return;
    }

    private void cmdDelete(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.delete")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        if (args.size() == 1) {
            String planName = args.pop();
            BuildingPlan plan = BuildInABox.getInstance().getDataStore().getBuildingPlan(planName);
            if (plan == null) {
                sender.sendMessage(BuildInABox.getErrorMsg("unknown-building-plan", planName));
            } else {
                BuildInABox.getInstance().getDataStore().deleteBuildingPlan(plan);
                sender.sendMessage(BuildInABox.getSuccessMsg("building-plan-deleted", planName));
            }
        } else {
            sender.sendMessage(BuildInABox.getNormalMsg("cmd-delete-usage"));
        }
        
    }

    private void cmdSetDescription(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        if (args.size() < 2) {
            sender.sendMessage(BuildInABox.getNormalMsg("cmd-setdesc-usage"));
            return;
        }
        BuildingPlan plan = BuildInABox.getInstance().getDataStore().getBuildingPlan(args.peek());
        if (plan == null) {
            sender.sendMessage(BuildInABox.getErrorMsg("unknown-building-plan", args.peek()));
            return;
        }
        args.pop();
        plan.description = parseDescription(args);
        BuildInABox.getInstance().getDataStore().saveBuildingPlan(plan);
        sender.sendMessage(BuildInABox.getSuccessMsg("description-saved", plan.getName()));
    }

    private void cmdSetDisplayName(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        if (args.size() < 2) {
            sender.sendMessage(BuildInABox.getNormalMsg("cmd-setdisplayname-usage"));
            return;
        }
        BuildingPlan plan = BuildInABox.getInstance().getDataStore().getBuildingPlan(args.peek());
        if (plan == null) {
            sender.sendMessage(BuildInABox.getErrorMsg("unknown-building-plan", args.peek()));
            return;
        }
        args.pop();
        String dn = "";
        while (!args.isEmpty()) {
            dn += convertColors(args.pop()) + " ";
        }
        dn = dn.trim();
        if (dn.equals("")) {
            dn = plan.getName();
        }
        plan.setDisplayName(dn);
        BuildInABox.getInstance().getDataStore().saveBuildingPlan(plan);
        sender.sendMessage(BuildInABox.getSuccessMsg("building-plan-saved", plan.getName()));
    }

    private String convertColors(String s) {
        Matcher m = colorPattern.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find())
            m.appendReplacement(sb, ChatColor.COLOR_CHAR + m.group(1).substring(1));
        m.appendTail(sb);
        return sb.toString();
    }

    private List<String> parseDescription(LinkedList<String> args) {
        List<String> lines = new ArrayList<String>();
        String line = "";
        String w;
        while (!args.isEmpty()) {
            w = args.pop().trim();
            if (w.equals("|")) {
                lines.add(line);
                line = "";
            } else {
                line += convertColors(w) + " ";
            }
        }
        if (!line.equals(" ")) {
            lines.add(line);
        }
        return lines;
    }
    private void cmdList(CommandSender sender, LinkedList<String> args) {
        int page = 1;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.peek());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(BuildInABox.getErrorMsg("invalid-page", args.peek()));
                return;
            }
        }
        int numPages = (int) Math.ceil(plugin.getDataStore().getAllBuildingPlans().size() / 8.0f);
        if (numPages == 0) {
            sender.sendMessage(BuildInABox.getNormalMsg("no-building-plans"));
            return;
        }
        List<BuildingPlan> plans = new ArrayList<BuildingPlan>(plugin.getDataStore().getAllBuildingPlans());
        List<String> lines = new ArrayList<String>();
        lines.add(BuildInABox.getNormalMsg("available-building-plans", page, numPages));
        for (int i=8*(page-1);i<8*(page);i++) {
            if (i<plans.size()) {
                lines.add(ChatColor.GOLD + " * " + ChatColor.GRAY + plans.get(i).getName() + " - " + plans.get(i).getDisplayName());
            }
        }
        sender.sendMessage(lines.toArray(new String[lines.size()]));
    }

    public void cmdSave(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(BuildInABox.getErrorMsg("cannot-use-from-console"));
            return;
        }
        String buildingName = args.pop();
        BuildingPlan plan = BuildingPlan.fromClipboard(plugin, (Player) sender, buildingName);
        String displayName = "";
        while (args.size() > 0 && !args.peek().equals("|")) {
            displayName += args.pop() + " ";
        }
        if (!displayName.equals("")) {
            plan.setDisplayName(displayName.trim());
        }
        if (args.size() > 0) {
            plan.setDescription(parseDescription(args));
        }
        if (plan != null) {
            sender.sendMessage(BuildInABox.getSuccessMsg("building-plan-saved", buildingName));
            plugin.getDataStore().saveBuildingPlan(plan);
        }
    }

    public void cmdGive(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.give")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        Player targetPlayer = null;
        if (args.size() >= 2) {
            String name = args.pop();
            List<Player> matches = plugin.getServer().matchPlayer(name);
            if (matches.size() == 1) {
                targetPlayer = matches.get(0);
            } else {
                sender.sendMessage(BuildInABox.getErrorMsg("unknown-player", name));
                return;
            }
        }
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(BuildInABox.getErrorMsg("cannot-give-to-console"));
                return;
            } else {
                targetPlayer = (Player) sender;
            }
        }
        if (args.size() == 1) {
            String planName = args.pop().toLowerCase();
            BuildingPlan plan = plugin.getDataStore().getBuildingPlan(planName);
            if (plan == null) {
                sender.sendMessage(BuildInABox.getErrorMsg("unknown-building-plan", planName));
                return;
            }
            ChestData data = plugin.getDataStore().createChest(plan.getName());
            ItemStack stack = data.toItemStack();
            if (targetPlayer.getInventory().addItem(stack).size() > 0) {
                targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), stack);
            }
            sender.sendMessage(BuildInABox.getSuccessMsg("cmd-give-success", data.getPlanName(), targetPlayer.getName()));
        } else {
            sender.sendMessage(BuildInABox.getNormalMsg("cmd-give-usage"));
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd,
            String label, String[] params) {
      LinkedList<String> args = new LinkedList<String>(Arrays.asList(params));
      LinkedList<String> results = new LinkedList<String>();
      String action = null;
      if (args.size() >= 1) {
          action = args.pop().toLowerCase();
      } else {
          return results;
      }
      
      if (args.size() == 0) {
          if ("permanent".startsWith(action) && sender.hasPermission("biab.permanent")) {
              results.add("permanent");
          }
          if ("setdescription".startsWith(action) && sender.hasPermission("biab.save")) {
              results.add("setdescription");
          }
          if ("list".startsWith(action)) {
              results.add("list");
          } 
          if ("save".startsWith(action) && sender.hasPermission("biab.save")) {
              results.add("save");
          }
          if ("give".startsWith(action) && sender.hasPermission("biab.give")) {
              results.add("give");
          }
      } else if (args.size() == 1) {
          if (action.equals("save") || action.equals("give") || action.equals("setdisplayname") || action.equals("setdescription")) {
              if (sender.hasPermission("biab." + action)) {
                  for (BuildingPlan plan: plugin.getDataStore().getAllBuildingPlans()) {
                      if (plan.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                          results.add(plan.getName());
                      }
                  }
                  if (action.equals("give")) {
                      for (Player p: plugin.getServer().getOnlinePlayers()) {
                          if (p.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                              results.add(p.getName());
                          }
                      }
                  }
              }
          }
      } else if (args.size() == 2) {
          if (action.equals("give")) {
              for (BuildingPlan plan: plugin.getDataStore().getAllBuildingPlans()) {
                  if (plan.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                      results.add(plan.getName());
                  }
              }
          }
      }
      return results;
    }
}
