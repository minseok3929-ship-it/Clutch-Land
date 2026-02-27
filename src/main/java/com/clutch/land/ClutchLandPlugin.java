package com.clutch.land;

import com.clutch.land.command.AdminLandCommand;
import com.clutch.land.command.PlayerLandCommand;
import com.clutch.land.infrastructure.AsyncDatabaseWriter;
import com.clutch.land.infrastructure.CacheLoader;
import com.clutch.land.infrastructure.Database;
import com.clutch.land.infrastructure.SqliteLandChunkRepository;
import com.clutch.land.infrastructure.SqliteLandMemberRepository;
import com.clutch.land.infrastructure.SqliteLandRepository;
import com.clutch.land.listener.ClaimTicketListener;
import com.clutch.land.listener.LandProtectionListener;
import com.clutch.land.listener.SelectionToolListener;
import com.clutch.land.repository.LandChunkRepository;
import com.clutch.land.repository.LandMemberRepository;
import com.clutch.land.repository.LandRepository;
import com.clutch.land.service.LandCacheService;
import com.clutch.land.service.LandCacheSnapshot;
import com.clutch.land.service.SelectionService;
import com.clutch.land.ui.MessageFacade;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClutchLandPlugin extends JavaPlugin {
    private Database database;
    private AsyncDatabaseWriter asyncWriter;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.database = new Database(getDataFolder().toPath().resolve("clutch-land.db"));
        this.database.connect();
        this.database.migrate();

        this.asyncWriter = new AsyncDatabaseWriter(this, database);
        this.asyncWriter.start();

        LandRepository landRepository = new SqliteLandRepository(database);
        LandMemberRepository landMemberRepository = new SqliteLandMemberRepository(database);
        LandChunkRepository landChunkRepository = new SqliteLandChunkRepository(database);

        CacheLoader cacheLoader = new CacheLoader(landRepository, landMemberRepository, landChunkRepository);
        LandCacheSnapshot snapshot = cacheLoader.loadAll();
        LandCacheService landCacheService = new LandCacheService(snapshot);

        SelectionService selectionService = new SelectionService();
        MessageFacade messageFacade = new MessageFacade();

        registerCommands(
                new AdminLandCommand(selectionService, landRepository, landChunkRepository, landCacheService, messageFacade),
                new PlayerLandCommand(landCacheService, landRepository, landMemberRepository, asyncWriter, messageFacade)
        );

        getServer().getPluginManager().registerEvents(new LandProtectionListener(landCacheService, messageFacade), this);
        getServer().getPluginManager().registerEvents(new SelectionToolListener(selectionService, messageFacade), this);
        getServer().getPluginManager().registerEvents(new ClaimTicketListener(landCacheService, landRepository, asyncWriter, messageFacade), this);

        getLogger().info("ClutchLand enabled.");
    }

    @Override
    public void onDisable() {
        if (asyncWriter != null) {
            asyncWriter.shutdownAndFlush();
        }

        if (database != null) {
            database.close();
        }

        getLogger().info("ClutchLand disabled.");
    }

    private void registerCommands(AdminLandCommand adminCommand, PlayerLandCommand playerCommand) {
        PluginCommand landCommand = Objects.requireNonNull(getCommand("토지"), "토지 command not found");
        PluginCommand plotCommand = Objects.requireNonNull(getCommand("땅"), "땅 command not found");

        landCommand.setExecutor(adminCommand);
        landCommand.setTabCompleter(adminCommand);
        plotCommand.setExecutor(playerCommand);
        plotCommand.setTabCompleter(playerCommand);
    }
}
