package com.clutch.land;

import com.clutch.land.command.LandAdminCommand;
import com.clutch.land.command.LandPlayerCommand;
import com.clutch.land.db.LandDatabase;
import com.clutch.land.item.LandItemFactory;
import com.clutch.land.listener.LandClaimListener;
import com.clutch.land.listener.LandEnterListener;
import com.clutch.land.listener.LandProtectionListener;
import com.clutch.land.listener.LandSelectionListener;
import com.clutch.land.manager.LandManager;
import com.clutch.land.manager.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ClutchLandPlugin extends JavaPlugin {
    public static final String ADMIN_PERMISSION = "clutchland.admin";
    public static String PREFIX = ChatColor.BLACK + "[CLUTCH] " + ChatColor.WHITE;

    private LandDatabase landDatabase;
    private LandManager landManager;
    private SelectionManager selectionManager;
    private LandItemFactory itemFactory;
    private boolean protectUnregisteredLand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPluginSettings();

        this.landDatabase = new LandDatabase(this);
        this.landDatabase.initialize();

        this.landManager = new LandManager(this, landDatabase);
        this.selectionManager = new SelectionManager();
        this.itemFactory = new LandItemFactory(this);

        registerCommands();
        registerListeners();

        getLogger().info("ClutchLand enabled.");
    }

    private void loadPluginSettings() {
        PREFIX = ChatColor.translateAlternateColorCodes('&',
            getConfig().getString("messages.prefix", "&0[CLUTCH] &f"));
        this.protectUnregisteredLand = getConfig().getBoolean("protection.protect-unregistered-land", true);
    }

    public boolean isProtectUnregisteredLand() {
        return protectUnregisteredLand;
    }

    @Override
    public void onDisable() {
        if (landDatabase != null) {
            landDatabase.close();
        }
        getLogger().info("ClutchLand disabled.");
    }

    private void registerCommands() {
        PluginCommand adminCommand = getCommand("토지");
        PluginCommand playerCommand = getCommand("땅");

        if (adminCommand != null) {
            adminCommand.setExecutor(new LandAdminCommand(landManager, selectionManager, itemFactory));
        } else {
            getLogger().severe("/토지 command is not defined in plugin.yml");
        }

        if (playerCommand != null) {
            playerCommand.setExecutor(new LandPlayerCommand(landManager));
        } else {
            getLogger().severe("/땅 command is not defined in plugin.yml");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new LandSelectionListener(selectionManager, itemFactory), this);
        getServer().getPluginManager().registerEvents(new LandClaimListener(landManager, itemFactory), this);
        getServer().getPluginManager().registerEvents(new LandProtectionListener(this, landManager), this);
        getServer().getPluginManager().registerEvents(new LandEnterListener(landManager), this);
    }
}
