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
package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.*;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedis;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class DeltaBans extends JavaPlugin
{
    private boolean debug;
    private Settings settings;

    private BanCommand banCommand;
    private BannedCommand bannedCommand;
    private KickCommand kickCommand;
    private NameBanCommand nameBanCommand;
    private RangeBanCommand rangeBanCommand;
    private RangeUnbanCommand rangeUnbanCommand;
    private RangeWhitelistCommand rangeWhitelistCommand;
    private SaveCommand saveCommand;
    private TempBanCommand tempBanCommand;
    private UnbanCommand unbanCommand;
    private UnwarnCommand unwarnCommand;
    private WarnCommand warnCommand;
    private DeltaBansListener deltaBansListener;

    @Override
    public void onLoad()
    {
        saveDefaultConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        debug = getConfig().getBoolean("DebugMode", false);
        settings = new Settings();
        settings.read(this);

        DeltaRedis plugin = (DeltaRedis) getServer().getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = plugin.getDeltaRedisApi();

        try
        {
            testConnection();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            severe("Failed to connect to database containing xAuth account table. Shutting down ...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        deltaBansListener = new DeltaBansListener(this);
        getServer().getPluginManager().registerEvents(deltaBansListener, this);

        banCommand = new BanCommand(deltaRedisApi, this);
        banCommand.register();

        bannedCommand = new BannedCommand(deltaRedisApi, this);
        bannedCommand.register();

        kickCommand = new KickCommand(deltaRedisApi, this);
        kickCommand.register();

        nameBanCommand = new NameBanCommand(deltaRedisApi, this);
        nameBanCommand.register();

        rangeBanCommand = new RangeBanCommand(deltaRedisApi, this);
        rangeBanCommand.register();

        rangeUnbanCommand = new RangeUnbanCommand(deltaRedisApi, this);
        rangeUnbanCommand.register();

        rangeWhitelistCommand = new RangeWhitelistCommand(deltaRedisApi, this);
        rangeWhitelistCommand.register();

        saveCommand = new SaveCommand(deltaRedisApi, this);
        saveCommand.register();

        tempBanCommand = new TempBanCommand(deltaRedisApi, this);
        tempBanCommand.register();

        unbanCommand = new UnbanCommand(deltaRedisApi, this);
        unbanCommand.register();

        unwarnCommand = new UnwarnCommand(deltaRedisApi, this);
        unwarnCommand.register();

        warnCommand = new WarnCommand(deltaRedisApi, this);
        warnCommand.register();
    }

    @Override
    public void onDisable()
    {
        deltaBansListener = null;

        warnCommand.shutdown();
        warnCommand = null;

        unwarnCommand.shutdown();
        unwarnCommand = null;

        unbanCommand.shutdown();
        unbanCommand = null;

        tempBanCommand.shutdown();
        tempBanCommand = null;

        saveCommand.shutdown();
        saveCommand = null;

        rangeUnbanCommand.shutdown();
        rangeUnbanCommand = null;

        rangeWhitelistCommand.shutdown();
        rangeWhitelistCommand = null;

        rangeBanCommand.shutdown();
        rangeBanCommand = null;

        nameBanCommand.shutdown();
        nameBanCommand = null;

        kickCommand.shutdown();
        kickCommand = null;

        bannedCommand.shutdown();
        bannedCommand = null;

        banCommand.shutdown();
        banCommand = null;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public String getIpOfPlayer(String playerName) throws IllegalArgumentException
    {
        try(Connection connection = DbShare.getDataSource(settings.getDatabase()).getConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(settings.getIpCheckQuery()))
            {
                statement.setString(1, playerName);
                try(ResultSet resultSet = statement.executeQuery())
                {
                    if(resultSet.next())
                    {
                        return resultSet.getString("lastloginip");
                    }
                    else
                    {
                        throw new IllegalArgumentException("There is no player by the name (" +
                            playerName + ") in the xAuth account table.");
                    }
                }
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            throw new IllegalArgumentException("Failed to access the xAuth accounts table.");
        }
    }

    public void info(String message)
    {
        getLogger().info(message);
    }

    public void severe(String message)
    {
        getLogger().severe(message);
    }

    public void debug(String message)
    {
        if(debug)
        {
            getLogger().info("[Debug] " + message);
        }
    }

    private void testConnection() throws SQLException
    {
        try(Connection connection = DbShare.getDataSource(settings.getDatabase()).getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                statement.execute("SELECT 1;");
            }
        }
    }
}
