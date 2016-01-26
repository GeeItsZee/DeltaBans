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
package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class GeneralListener implements Listener, Registerable, Shutdownable
{
    private BanStorage banStorage;
    private WarningStorage warningStorage;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public GeneralListener(DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.banStorage = plugin.getBanStorage();
        this.warningStorage = plugin.getWarningStorage();
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void unregister()
    {
        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @Override
    public void shutdown()
    {
        banStorage = null;
        warningStorage = null;
        deltaRedisApi = null;
        plugin = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        byte[] messageBytes = event.getMessage().getBytes(StandardCharsets.UTF_8);
        ByteArrayDataInput in = ByteStreams.newDataInput(messageBytes);

        if(channel.equals(DeltaBansChannels.BANNED))
        {
            String sender = in.readUTF();
            String argument = in.readUTF();
            boolean isIp = in.readBoolean();
            boolean hasExtra = in.readBoolean();
            StringBuilder builder = new StringBuilder();

            if(isIp)
            {
                Set<BanEntry> entries = banStorage.getIpBanEntries(argument);

                builder.append(Prefixes.INFO).append("Bans found:");
                for(BanEntry entry : entries)
                {
                    String infoString = getBanInfoFor(entry, hasExtra);
                    builder.append("\n").append(infoString);
                }
            }
            else
            {
                BanEntry entry = banStorage.getNameBanEntry(argument.toLowerCase());
                String banInfoString = getBanInfoFor(entry, hasExtra);
                String warningInfoString = getWarningInfoFor(argument);
                builder.append(banInfoString).append("\n").append(warningInfoString);
            }

            deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender, builder.toString());
        }
        else if(channel.equals(DeltaBansChannels.SAVE))
        {
            String sender = event.getMessage();
            if(plugin.writeBansAndWarnings())
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender,
                    Prefixes.SUCCESS + "Ban and warning files saved.");
            }
            else
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender,
                    Prefixes.FAILURE + "Error saving files. More details in the BungeeCord console.");
            }
        }
    }

    private String getBanInfoFor(BanEntry entry, boolean hasExtra)
    {
        if(entry == null)
        {
            return Prefixes.INFO + "Ban not found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(Prefixes.INFO).append("Ban found\n");
        builder.append("  Name: ").append(Prefixes.input(entry.getName())).append("\n");

        if(hasExtra)
        {
            builder.append("  IP: ").append(Prefixes.input(entry.getIp())).append("\n");
        }

        builder.append("  Banner: ").append(Prefixes.input(entry.getBanner())).append("\n");
        builder.append("  Ban Message: ").append(Prefixes.input(entry.getMessage())).append("\n");
        builder.append("  Duration: ").append(Prefixes.input(
            DeltaBansUtils.formatDuration(entry.getDuration())));

        return builder.toString();
    }

    private String getWarningInfoFor(String name)
    {
        List<WarningEntry> warnings = warningStorage.getWarnings(name);
        StringBuilder builder = new StringBuilder(Prefixes.INFO + "Warnings for " +
            Prefixes.input(name));

        for(WarningEntry entry : warnings)
        {
            builder.append("\n");
            builder.append(" - ").append(entry.getMessage());
            builder.append(" (").append(entry.getWarner()).append(")");
        }

        return builder.toString();
    }
}