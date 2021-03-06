/*
 * This file is part of DeltaBans.
 *
 * DeltaBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaBans.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBans;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class KickCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public KickCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("kick").setExecutor(this);
        plugin.getCommand("kick").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("kick").setExecutor(null);
        plugin.getCommand("kick").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(formatUsage("/kick <name> [message]"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Kick"))
        {
            sender.sendMessage(formatNoPerm("DeltaBans.Kick"));
            return true;
        }

        String kicker = sender.getName();
        String nameToKick = args[0];
        if(kicker.equalsIgnoreCase(nameToKick))
        {
            sender.sendMessage(format("DeltaBans.NotAllowedToSelf", "kick"));
            return true;
        }

        String message = null;
        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.KICK,
            kicker,
            nameToKick,
            message == null ? "" : message,
            isSilent ? "1" : "0");
        return true;
    }
}
