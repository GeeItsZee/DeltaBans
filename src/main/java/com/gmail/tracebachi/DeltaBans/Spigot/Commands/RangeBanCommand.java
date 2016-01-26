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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class RangeBanCommand implements CommandExecutor, Registerable, Shutdownable
{
    private static final Pattern DASH_PATTERN = Pattern.compile("-");

    private String defaultRangeBanMessage;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public RangeBanCommand(String defaultRangeBanMessage, DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.defaultRangeBanMessage = defaultRangeBanMessage;
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("rangeban").setExecutor(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("rangeban").setExecutor(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        defaultRangeBanMessage = null;
        deltaRedisApi = null;
        plugin = null;
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
            sender.sendMessage(Prefixes.INFO + "/rangeban <ip>-<ip> [message]");
            return true;
        }

        if(!sender.hasPermission("DeltaBans.RangeBan"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaBans.RangeBan") + " permission.");
            return true;
        }

        String banner = sender.getName();
        String message = defaultRangeBanMessage;
        String[] splitIpRange = DASH_PATTERN.split(args[0]);

        if(splitIpRange.length != 2)
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(args[0]) + " is not a valid IP range.");
            return true;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[0]))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(splitIpRange[0]) + " is not a valid IP.");
            return true;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[1]))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(splitIpRange[1]) + " is not a valid IP.");
            return true;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        long firstAsLong = DeltaBansUtils.convertIpToLong(splitIpRange[0]);
        long secondAsLong = DeltaBansUtils.convertIpToLong(splitIpRange[1]);

        if(firstAsLong == secondAsLong)
        {
            sender.sendMessage(Prefixes.FAILURE + "Use an IP ban instead.");
        }
        else if(firstAsLong > secondAsLong)
        {
            String channelMessage = buildChannelMessage(banner, message,
                splitIpRange[1], splitIpRange[0], isSilent);
            deltaRedisApi.publish(Servers.BUNGEECORD, DeltaBansChannels.RANGE_BAN, channelMessage);
        }
        else
        {
            String channelMessage = buildChannelMessage(banner, message,
                splitIpRange[0], splitIpRange[1], isSilent);
            deltaRedisApi.publish(Servers.BUNGEECORD, DeltaBansChannels.RANGE_BAN, channelMessage);
        }

        return true;
    }

    private String buildChannelMessage(String name, String message, String start, String end, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(name);
        out.writeUTF(message);
        out.writeUTF(start);
        out.writeUTF(end);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}