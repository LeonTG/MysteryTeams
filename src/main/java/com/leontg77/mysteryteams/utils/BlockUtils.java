package com.leontg77.mysteryteams.utils;

import com.leontg77.mysteryteams.Main;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Block utilities class.
 * 
 * @author LeonTG
 */
public class BlockUtils {
    private static Main plugin;

    public static void setPlugin(Main plugin) {
        Validate.notNull(plugin, "Plugin cannot be null.");

        BlockUtils.plugin = plugin;
    }

    private static final Random RANDOM = new Random();

    /**
     * Drop the given item at the given location as if a normal block break would drop it.
     *
     * @param dropLoc The location dropping at.
     * @param toDrop The item dropping.
     */
    static void dropItem(Location dropLoc, ItemStack toDrop) {
        Validate.notNull(dropLoc, "Location cannot be null.");
        Validate.notNull(toDrop, "ItemStack cannot be null.");

        // This method uses the 1.7 item dropping code bukkit has, since the 1.8 code makes the item fly everywhere.

        Location loc = dropLoc.clone();

        double xs = RANDOM.nextFloat() * 0.7F + (1.0F - 0.7F) * 0.5D;
        double ys = RANDOM.nextFloat() * 0.7F + (1.0F - 0.7F) * 0.5D;
        double zs = RANDOM.nextFloat() * 0.7F + (1.0F - 0.7F) * 0.5D;

        loc = loc.clone();
        loc.setX(loc.getX() + xs);
        loc.setY(loc.getY() + ys);
        loc.setZ(loc.getZ() + zs);

        Location finalLoc = loc.clone();

        Bukkit.getScheduler().runTaskLater(plugin, () -> finalLoc.getWorld().dropItem(finalLoc, toDrop), 2L);
    }
}