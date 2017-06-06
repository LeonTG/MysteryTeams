package com.leontg77.mysteryteams.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.leontg77.mysteryteams.Main;
import com.leontg77.mysteryteams.Main.MysteryTeam;
import com.leontg77.mysteryteams.utils.PlayerUtils;
import com.leontg77.mysteryteams.utils.StringUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.leontg77.mysteryteams.Main.PREFIX;

/**
 * Command class.
 *
 * @author Leon
 */
public class MTCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public MTCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            helpMenu(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("teamsize")) {
            sender.sendMessage(PREFIX + "All teamsizes:");

            for (MysteryTeam team : plugin.currentTeams) {
                sender.sendMessage(PREFIX + team.getChatColor() + "Team " + team.getName().toLowerCase() + "'s teamsize: §f" + team.getSize());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (sender instanceof Player && ((Player) sender).getGameMode() == GameMode.SPECTATOR) {
                helpMenu(sender);
                return true;
            }

            if (plugin.originalTeams.isEmpty()) {
                sender.sendMessage(PREFIX + "There are no mystery teams.");
                return true;
            }

            sender.sendMessage(PREFIX + "All Mystery Teams: §8(§f§oItalic §f= Dead§8)");

            for (Map.Entry<MysteryTeam, Set<UUID>> entry : plugin.originalTeams.entrySet()) {
                StringBuilder members = new StringBuilder();
                int i = 1;

                MysteryTeam team = entry.getKey();
                Set<UUID> uuids = entry.getValue();

                for (UUID member : uuids) {
                    if (members.length() > 0) {
                        if (i == uuids.size()) {
                            members.append(" §8and §f");
                        } else {
                            members.append("§8, §f");
                        }
                    }

                    OfflinePlayer offline = Bukkit.getOfflinePlayer(member);

                    if (plugin.currentTeams.contains(team)) {
                        members.append(team.hasPlayer(offline) ? ChatColor.WHITE + offline.getName() : ChatColor.ITALIC + offline.getName());
                    } else {
                        members.append(ChatColor.ITALIC).append(offline.getName());
                    }

                    i++;
                }

                sender.sendMessage(team.getChatColor() + team.getName() + ": §f" + members.toString().trim());
            }
            return true;
        }

        if (!sender.hasPermission("mt.admin")) {
            helpMenu(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /mt add <team> <player>");
                return true;
            }

            MysteryTeam team = null;

            for (MysteryTeam teams : plugin.currentTeams) {
                if (teams.getName().replaceAll(" ", "").equalsIgnoreCase(args[1])) {
                    team = teams;
                    break;
                }
            }

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid team.");
                return true;
            }

            if (!plugin.originalTeams.containsKey(team)) {
                plugin.originalTeams.put(team, Sets.newHashSet());
            }

            if (!plugin.currentTeams.contains(team)) {
                plugin.currentTeams.add(team);
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[2]);
            sender.sendMessage(PREFIX + ChatColor.GREEN + offline.getName() + "§7 was added to team §6" + team.getName() + "§7.");

            plugin.originalTeams.get(team).add(offline.getUniqueId());
            team.addPlayer(offline);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /mt remove <player>");
                return true;
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            MysteryTeam team = plugin.getTeam(offline);

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid team.");
                return true;
            }

            if (!plugin.originalTeams.containsKey(team)) {
                plugin.originalTeams.put(team, Sets.newHashSet());
            }

            sender.sendMessage(PREFIX + ChatColor.GREEN + offline.getName() + " §7was removed from his team.");

            plugin.originalTeams.get(team).remove(offline.getUniqueId());
            team.removePlayer(offline);
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /mt delete <team>");
                return true;
            }

            MysteryTeam team = null;

            for (MysteryTeam teams : plugin.currentTeams) {
                if (teams.getName().replaceAll(" ", "").equalsIgnoreCase(args[1])) {
                    team = teams;
                    break;
                }
            }

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not on a team.");
                return true;
            }

            if (plugin.originalTeams.containsKey(team)) {
                plugin.originalTeams.remove(team);
            }

            if (plugin.currentTeams.contains(team)) {
                plugin.currentTeams.remove(team);
            }

            for (OfflinePlayer teammate : team.getPlayers()) {
                team.removePlayer(teammate);
            }

            sender.sendMessage(PREFIX + "Team §a" + team.getName() + " §7has been deleted.");
            return true;
        }

        if (args[0].equalsIgnoreCase("mode")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /mt mode <newmode>");
                return true;
            }

            Material newMode = null;

            for (Material type : Material.values()) {
                if (type.name().startsWith(args[1].toUpperCase())) {
                    newMode = type;
                    break;
                }
            }

            if (newMode == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid mode.");
                return true;
            }

            switch (newMode) {
                case FIREWORK:
                case BANNER:
                case WOOL:
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid mode.");
                    return true;
            }

            plugin.mode = newMode;
            PlayerUtils.broadcast(PREFIX + "Mystery Teams mode has been changed to " + newMode.name().toLowerCase() + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            for (MysteryTeam team : plugin.currentTeams) {
                team.getPlayers().forEach(team::removePlayer);
            }

            plugin.originalTeams.clear();
            plugin.currentTeams.clear();

            sender.sendMessage(PREFIX + "All teams have been cleared.");
            return true;
        }

        if (args[0].equalsIgnoreCase("randomize")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /mt randomize <teamsize> <amount of teams>");
                return true;
            }

            int teamSize = Integer.parseInt(args[1]);
            int amount = Integer.parseInt(args[2]);

            for (int i = 0; i < amount; i++) {
                List<Player> players = Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(online -> plugin.getTeam(online) == null)
                        .filter(online -> online.getGameMode() != GameMode.SPECTATOR)
                        .collect(Collectors.toList());

                MysteryTeam team = plugin.findAvailableTeam();

                if (team == null) {
                    sender.sendMessage(ChatColor.RED + "There are no more available teams.");
                    break;
                }

                if (!plugin.originalTeams.containsKey(team)) {
                    plugin.originalTeams.put(team, Sets.newHashSet());
                }

                if (!plugin.currentTeams.contains(team)) {
                    plugin.currentTeams.add(team);
                }

                for (int j = 0; j < teamSize; j++) {
                    if (players.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "No more players to add to the team.");
                        break;
                    }

                    Player player = players.remove(0);

                    plugin.originalTeams.get(team).add(player.getUniqueId());
                    team.addPlayer(player);
                }
            }

            PlayerUtils.broadcast(PREFIX + "Randomized " + amount + " teams of " + teamSize + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 1) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    MysteryTeam team = plugin.getTeam(online);

                    if (team == null) {
                        continue;
                    }

                    ItemStack item = plugin.getItem(team);
                    PlayerUtils.giveItem(online, item);
                }

                PlayerUtils.broadcast(PREFIX + StringUtils.fix(plugin.mode.name(), true) + "s have been given to all players.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not online.");
                return true;
            }

            MysteryTeam team = plugin.getTeam(target);

            if (team == null) {
                sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not on a team.");
                return true;
            }

            ItemStack item = plugin.getItem(team);

            sender.sendMessage(PREFIX + StringUtils.fix(plugin.mode.name(), true) + "s have been given to " + target.getName() + ".");
            PlayerUtils.giveItem(target, item);
            return true;
        }

        helpMenu(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> toReturn = Lists.newArrayList();

        if (args.length == 1) {
            toReturn.add("teamsize");

            if (sender.hasPermission("mt.admin")) {
                toReturn.add("list");
                toReturn.add("remove");
                toReturn.add("delete");
                toReturn.add("mode");
                toReturn.add("clear");
                toReturn.add("randomize");
                toReturn.add("give");
            }
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "add":
                case "delete":
                    toReturn.addAll(Arrays.stream(MysteryTeam.values())
                            .map(mt -> mt.getName().toLowerCase().replaceAll(" ", ""))
                            .collect(Collectors.toList()));
                    break;
                case "remove":
                case "give":
                    return null;
                case "mode":
                    toReturn.add("firework");
                    toReturn.add("banner");
                    toReturn.add("wool");
                    break;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return null;
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], toReturn, Lists.newArrayList());
    }

    /**
     * Print the help menu to the given sender.
     *
     * @param sender The sender printing too
     */
    private void helpMenu(CommandSender sender) {
        sender.sendMessage(PREFIX + "MysteryTeams Help Menu:");
        sender.sendMessage("§8- §f/mt teamsize §8- §7§oDisplay all teams current teamsize.");

        if (!sender.hasPermission("mt.admin")) {
            return;
        }

        sender.sendMessage("§8- §f/mt list §8- §7§oDisplay all teams.");
        sender.sendMessage("§8- §f/mt add <team> <player> §8- §7§oAdd the player to the team.");
        sender.sendMessage("§8- §f/mt remove <player> §8- §7§oRemove the player from his team.");
        sender.sendMessage("§8- §f/mt delete <team> §8- §7§oRemove all players from the team.");
        sender.sendMessage("§8- §f/mt mode <new mode> §8- §7§oChange the mystery team item to use.");
        sender.sendMessage("§8- §f/mt clear §8- §7§oClear all teams.");
        sender.sendMessage("§8- §f/mt randomize <teamsize> <amount of teams> §8- §7§oRandomize the teams.");
        sender.sendMessage("§8- §f/mt give [player] §8- §7§oGive the banners to the given player or everyone.");
    }
}