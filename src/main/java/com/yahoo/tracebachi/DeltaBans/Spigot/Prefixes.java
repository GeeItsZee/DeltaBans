package com.yahoo.tracebachi.DeltaBans.Spigot;

import org.bukkit.ChatColor;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public interface Prefixes
{
    String INFO = ChatColor.translateAlternateColorCodes('&',
        "&8[&9!&8] &9Info &8[&9!&8]&7 ");

    String SUCCESS = ChatColor.translateAlternateColorCodes('&',
        "&8[&a!&8] &aSuccess &8[&a!&8]&7 ");

    String FAILURE = ChatColor.translateAlternateColorCodes('&',
        "&8[&c!&8] &cFailure &8[&c!&8]&7 ");
}
