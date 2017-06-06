package com.leontg77.mysteryteams.utils;

import com.leontg77.mysteryteams.Main;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Player utilities class.
 * 
 * @author LeonTG77
 */
public class PlayerUtils {

    /**
     * Broadcasts a message to everyone online with a specific rank.
     *
     * @param message The message to broadcast.
     */
    public static void broadcast(String message) {
        if (message == null) {
            return;
        }

        // I know Bukkit#broadcastMessage exist but it doesn't seem to work correctly after reloads.

        Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(message));
        Bukkit.getLogger().info(StringUtils.removeFormatColors(message));
    }

    /**
     * Give the given item to the given player, if the player's inventory is full it will drop to the ground.
     *
     * @param player The player giving to.
     * @param stacks The items to give.
     */
    public static void giveItem(Player player, ItemStack... stacks) {
        Validate.notNull(player, "Player cannot be null.");
        Validate.notNull(stacks, "ItemStack array cannot be null.");

        Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(stacks);

        if (leftOvers.isEmpty()) {
            return;
        }

        player.sendMessage(Main.PREFIX + "Inventory full! Item(s) were dropped on the ground.");
        Location loc = player.getLocation();

        for (ItemStack leftOver : leftOvers.values()) {
            BlockUtils.dropItem(loc, leftOver);
        }
    }
}