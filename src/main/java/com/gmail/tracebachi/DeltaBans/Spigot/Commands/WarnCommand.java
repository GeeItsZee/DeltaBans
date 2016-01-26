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

import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBans;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class WarnCommand implements TabExecutor, Registerable, Shutdownable
{
    private String defaultWarningMessage;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public WarnCommand(String defaultWarningMessage, DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.defaultWarningMessage = defaultWarningMessage;
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("warn").setExecutor(this);
        plugin.getCommand("warn").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("warn").setExecutor(null);
        plugin.getCommand("warn").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        defaultWarningMessage = null;
        deltaRedisApi = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
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
            sender.sendMessage(Prefixes.INFO + "/warn <name> [message]");
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Warn"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaBans.Warn") + " permission.");
            return true;
        }

        String warner = sender.getName();
        String name = args[0];
        String message = defaultWarningMessage;

        if(name.equalsIgnoreCase(warner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Warning yourself? You have been warned! :)");
            return true;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        String channelMessage = buildChannelMessage(warner, name, message, isSilent);
        deltaRedisApi.publish(Servers.BUNGEECORD, DeltaBansChannels.WARN, channelMessage);

        return true;
    }

    private String buildChannelMessage(String warner, String name, String message, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(warner);
        out.writeUTF(name);
        out.writeUTF(message);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}