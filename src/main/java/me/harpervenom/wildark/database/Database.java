package me.harpervenom.wildark.database;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.database.managers.BlocksManager;
import me.harpervenom.wildark.database.managers.LocksManager;
import me.harpervenom.wildark.database.managers.PlayersManager;
import me.harpervenom.wildark.database.managers.RegionsManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private Connection connection;

    public PlayersManager players;
    public BlocksManager blocks;
    public LocksManager locks;
    public RegionsManager regions;

    public void init() {
        try {
            if (!WILDARK.getPlugin().getDataFolder().exists()) {
                WILDARK.getPlugin().getDataFolder().mkdirs();
            }
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + WILDARK.getPlugin().getDataFolder().getAbsolutePath()
                            + "/wildark.db");

            players = new PlayersManager(connection);
            blocks = new BlocksManager(connection);
            locks = new LocksManager(connection);
            regions = new RegionsManager(connection);

        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()){
                connection.close();
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
