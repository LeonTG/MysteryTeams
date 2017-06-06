package com.leontg77.mysteryteams;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.leontg77.mysteryteams.commands.MTCommand;
import com.leontg77.mysteryteams.utils.BlockUtils;
import com.leontg77.mysteryteams.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MysteryTeams scenario class
 *
 * @author EXSolo, modified by LeonTG77
 */
public class Main extends JavaPlugin implements Listener {
    public static final String PREFIX = "§6[§cMysteryTeams§6] §f";

    public final Map<MysteryTeam, Set<UUID>> originalTeams = Maps.newHashMap();
    public final List<MysteryTeam> currentTeams = Lists.newArrayList();

    public Material mode = Material.BANNER;

    @Override
    public void onEnable() {
        BlockUtils.setPlugin(this);
        getCommand("mt").setExecutor(new MTCommand(this));

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    MysteryTeam team = getTeam(online);

                    if (team == null) {
                        continue;
                    }

                    List<Player> teamMembers = team.getPlayers()
                            .stream()
                            .filter(OfflinePlayer::isOnline)
                            .map(OfflinePlayer::getPlayer)
                            .collect(Collectors.toList());


                    if (teamMembers.isEmpty()) {
                        continue;
                    }

                    teamMembers.removeIf(i -> i.getUniqueId().equals(online.getUniqueId()));

                    Collections.sort(teamMembers, (o1, o2) -> Double.compare(
                            online.getLocation().distance(o1.getLocation()),
                            online.getLocation().distance(o2.getLocation())
                    ));

                    Player player = teamMembers.get(0);
                    online.setCompassTarget(player.getLocation());
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    @EventHandler
    public void on(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();

        if (result == null) {
            return;
        }

        if (result.getType() == mode) {
            inv.setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void on(PlayerShearEntityEvent event) {
        if (mode != Material.WOOL) {
            return;
        }

        Entity entity = event.getEntity();

        if (entity instanceof Sheep) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void on(EntityDeathEvent event) {
        if (mode != Material.WOOL) {
            return;
        }

        Entity entity = event.getEntity();

        if (entity instanceof Sheep) {
            event.getDrops()
                    .stream()
                    .filter(drop -> drop.getType() == Material.WOOL)
                    .forEach(drop -> drop.setType(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerDeathEvent event) {
        Player player = event.getEntity();
        MysteryTeam team = getTeam(player);

        event.getDrops()
                .stream()
                .filter(drop -> drop.getType() == mode)
                .forEach(drop -> drop.setType(Material.AIR));

        if (team == null) {
            return;
        }

        team.removePlayer(player);

        PlayerUtils.broadcast(PREFIX + team.getChatColor() + "Player from team " + team.getName() + " died.");

        if (team.getSize() > 0) {
            PlayerUtils.broadcast(PREFIX + team.getChatColor() + "Team " + team.getName() + " has §f" + team.getSize() + team.getChatColor() + (team.getSize() > 1 ? " players" : " player") + " left");
            return;
        }

        currentTeams.remove(team);

        if (currentTeams.size() > 1) {
            PlayerUtils.broadcast(PREFIX + team.getChatColor() + "Team " + team.getName() + " eliminated. There are §f" + currentTeams.size() + team.getChatColor() + " teams left.");
            return;
        }

        Optional<MysteryTeam> killerTeam = currentTeams.stream().findAny();

        if (!killerTeam.isPresent()) {
            return;
        }

        PlayerUtils.broadcast(PREFIX + team.getChatColor() + "Team " + team.getName() + " eliminated. " + killerTeam.get().getChatColor() + "The " + killerTeam.get().getName() + " team won!");
    }

    /**
     * Get the item to give players to find teammates.
     *
     * @param team The team to get the color from.
     * @return The item to give.
     */
    public ItemStack getItem(MysteryTeam team) {
        switch (mode) {
            case FIREWORK:
                ItemStack firework = new ItemStack(mode, 64);
                FireworkMeta fireworkMeta = (FireworkMeta) firework.getItemMeta();
                fireworkMeta.addEffect(FireworkEffect.builder().withColor(team.getDyeColor().getColor()).build());

                FireworkEffect effects = FireworkEffect.builder().withColor(team.getDyeColor().getFireworkColor()).with(FireworkEffect.Type.BALL_LARGE).build();

                fireworkMeta.addEffect(effects);
                fireworkMeta.setPower(2);

                firework.setItemMeta(fireworkMeta);
                return firework;
            case BANNER:
                ItemStack banner = new ItemStack(mode, 1);
                BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
                bannerMeta.setBaseColor(team.getDyeColor());
                banner.setItemMeta(bannerMeta);
                return banner;
            case WOOL:
                return new Wool(team.getDyeColor()).toItemStack(1);
            default:
                return null;
        }
    }

    /**
     * Register a new mystery team.
     *
     * @return The mystery team created.
     */
    public MysteryTeam findAvailableTeam() {
        List<MysteryTeam> list = Arrays.asList(MysteryTeam.values());
        Collections.shuffle(list);

        for (MysteryTeam team : list) {
            if (team.getSize() == 0) {
                return team;
            }
        }

        return null;
    }

    /**
     * Get the mystery team of the given player.
     *
     * @param offline The player to check with.
     * @return The team if any, null otherwise.
     */
    public MysteryTeam getTeam(OfflinePlayer offline) {
        for (MysteryTeam team : currentTeams) {
            if (team.hasPlayer(offline)) {
                return team;
            }
        }

        return null;
    }

    public enum MysteryTeam {
        /**
         * Green team.
         */
        GREEN("Green", ChatColor.DARK_GREEN, DyeColor.GREEN),
        /**
         * Orange team.
         */
        ORANGE("Orange", ChatColor.GOLD, DyeColor.ORANGE),
        /**
         * Red team.
         */
        RED("Red", ChatColor.RED, DyeColor.RED),
        /**
         * Light blue team.
         */
        LIGHT_BLUE("Light blue", ChatColor.AQUA, DyeColor.LIGHT_BLUE),
        /**
         * Yellow team.
         */
        YELLOW("Yellow", ChatColor.YELLOW, DyeColor.YELLOW),
        /**
         * Light Green team.
         */
        LIME("Light Green", ChatColor.GREEN, DyeColor.LIME),
        /**
         * Pink team.
         */
        PINK("Pink", ChatColor.LIGHT_PURPLE, DyeColor.PINK),
        /**
         * Gray team.
         */
        GRAY("Gray", ChatColor.DARK_GRAY, DyeColor.GRAY),
        /**
         * Light Gray team.
         */
        SILVER("Light Gray", ChatColor.GRAY, DyeColor.SILVER),
        /**
         * Purple team.
         */
        PURPLE("Purple", ChatColor.DARK_PURPLE, DyeColor.PURPLE),
        /**
         * Blue team.
         */
        BLUE("Blue", ChatColor.BLUE, DyeColor.BLUE),
        /**
         * Cyan team.
         */
        CYAN("Cyan", ChatColor.DARK_AQUA, DyeColor.CYAN),
        /**
         * White team.
         */
        WHITE("White", ChatColor.WHITE, DyeColor.WHITE),
        /**
         * Black team.
         */
        BLACK("Black", ChatColor.BLACK, DyeColor.BLACK);

        private final String name;

        private final ChatColor chat;
        private final DyeColor dye;

        /**
         * Mystery Team class constructor.
         *
         * @param name The team name.
         * @param chat The team chat color.
         * @param dye The team dye color.
         */
        MysteryTeam(String name, ChatColor chat, DyeColor dye) {
            this.name = name;

            this.chat = chat;
            this.dye = dye;
        }

        private final Set<UUID> players = Sets.newHashSet();

        /**
         * Get the name of the team.
         *
         * @return The name.
         */
        public String getName() {
            return name;
        }

        /**
         * Get the chat color for the team.
         *
         * @return The chat color.
         */
        public ChatColor getChatColor() {
            return chat;
        }

        /**
         * Get the dye color for the team.
         *
         * @return The dye color.
         */
        public DyeColor getDyeColor() {
            return dye;
        }

        /**
         * Get a set of all players on this team.
         *
         * @return A set of players.
         */
        public Set<OfflinePlayer> getPlayers() {
            return ImmutableSet.copyOf(players
                    .stream()
                    .map(Bukkit::getOfflinePlayer)
                    .collect(Collectors.toSet()));
        }

        /**
         * Get the size of the team.
         *
         * @return The size.
         */
        public int getSize() {
            return players.size();
        }

        /**
         * Add the given player to the team.
         *
         * @param player The player adding.
         */
        public void addPlayer(OfflinePlayer player) {
            players.add(player.getUniqueId());
        }

        /**
         * Remove the given player to the team.
         *
         * @param player The player removing.
         * @return True if successful, false otherwise.
         */
        public boolean removePlayer(OfflinePlayer player) {
            return players.remove(player.getUniqueId());
        }

        /**
         * Check if the given player is on this team.
         *
         * @param player The player checking.
         * @return True if he is, false otherwise.
         */
        public boolean hasPlayer(OfflinePlayer player) {
            return players.contains(player.getUniqueId());
        }
    }
}