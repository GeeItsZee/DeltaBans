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
package com.yahoo.tracebachi.DeltaBans.Spigot;

import com.yahoo.tracebachi.DeltaBans.Spigot.Commands.*;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class DeltaBansPlugin extends JavaPlugin
{
    private boolean debug;
    private String username;
    private String password;
    private String url;
    private String ipCheckQuery;

    private BanCommand banCommand;
    private BannedCommand bannedCommand;
    private NameBanCommand nameBanCommand;
    private SaveCommand saveCommand;
    private TempBanCommand tempBanCommand;
    private UnbanCommand unbanCommand;
    private UnwarnCommand unwarnCommand;
    private WarnCommand warnCommand;
    private DeltaBansListener deltaBansListener;

    private Connection connection;

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
        username = getConfig().getString("Database.Username");
        password = getConfig().getString("Database.Password");
        url = getConfig().getString("Database.URL");

        String accountTable = getConfig().getString("xAuth-AccountsTable");
        String defaultBanMessage = getConfig().getString("DefaultBanMessage");
        String defaultTempBanMessage = getConfig().getString("DefaultTempBanMessage");
        String defaultWarningMessage = getConfig().getString("DefaultWarningMessage");
        ipCheckQuery = "SELECT lastloginip FROM `" + accountTable + "` WHERE playername = ?;";

        DeltaRedisPlugin plugin = (DeltaRedisPlugin) getServer().getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = plugin.getDeltaRedisApi();

        try
        {
            updateConnection();
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

        banCommand = new BanCommand(defaultBanMessage, deltaRedisApi, this);
        banCommand.register();

        tempBanCommand = new TempBanCommand(defaultTempBanMessage, deltaRedisApi, this);
        tempBanCommand.register();

        nameBanCommand = new NameBanCommand(defaultBanMessage, deltaRedisApi, this);
        nameBanCommand.register();

        unbanCommand = new UnbanCommand(deltaRedisApi, this);
        unbanCommand.register();

        bannedCommand = new BannedCommand(deltaRedisApi, this);
        bannedCommand.register();

        warnCommand = new WarnCommand(defaultWarningMessage, deltaRedisApi, this);
        warnCommand.register();

        unwarnCommand = new UnwarnCommand(deltaRedisApi, this);
        unwarnCommand.register();

        bannedCommand = new BannedCommand(deltaRedisApi, this);
        bannedCommand.register();

        saveCommand = new SaveCommand(deltaRedisApi, this);
        saveCommand.register();
    }

    @Override
    public void onDisable()
    {
        deltaBansListener = null;

        if(saveCommand != null)
        {
            saveCommand.shutdown();
            saveCommand = null;
        }

        if(bannedCommand != null)
        {
            bannedCommand.shutdown();
            bannedCommand = null;
        }

        if(unwarnCommand != null)
        {
            unwarnCommand.shutdown();
            unwarnCommand = null;
        }

        if(warnCommand != null)
        {
            warnCommand.shutdown();
            warnCommand = null;
        }

        if(bannedCommand != null)
        {
            bannedCommand.shutdown();
            bannedCommand = null;
        }

        if(nameBanCommand != null)
        {
            nameBanCommand.shutdown();
            nameBanCommand = null;
        }

        if(tempBanCommand != null)
        {
            tempBanCommand.shutdown();
            tempBanCommand = null;
        }

        if(unbanCommand != null)
        {
            unbanCommand.shutdown();
            unbanCommand = null;
        }

        if(banCommand != null)
        {
            banCommand.shutdown();
            banCommand = null;
        }

        try
        {
            connection.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public String getIpOfPlayer(String playerName) throws IllegalArgumentException
    {
        try
        {
            updateConnection();
            try(PreparedStatement statement = connection.prepareStatement(ipCheckQuery))
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

    private void updateConnection() throws SQLException
    {
        if(connection == null)
        {
            debug("Establishing connection with DB for xAuth table ...");
            connection = DriverManager.getConnection("jdbc:mysql://" + url, username, password);
            debug("Establishing connection with DB for xAuth table ... Done");
        }
        else if(!connection.isValid(1))
        {
            debug("Reconnecting with DB for xAuth table ...");
            connection.close();
            connection = DriverManager.getConnection("jdbc:mysql://" + url, username, password);
            debug("Reconnecting with DB for xAuth table ... Done");
        }
    }
}
