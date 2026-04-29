package com.clutch.land.listener;

import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LandEnterListener implements Listener {
    private static final String TITLE = ChatColor.BLACK + "CLUTCH";

    private final LandManager landManager;
    private final Map<UUID, Land> currentLandMap = new HashMap<>();

    public LandEnterListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
            && Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Land previousLand = currentLandMap.get(uuid);
        Land currentLand = landManager.getLandAt(event.getTo()).orElse(null);

        if (isSameLand(previousLand, currentLand)) {
            return;
        }

        if (previousLand != null) {
            showExitTitle(player, previousLand);
        }

        if (currentLand != null) {
            showEnterTitle(player, currentLand);
            currentLandMap.put(uuid, currentLand);
        } else {
            currentLandMap.remove(uuid);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentLandMap.remove(event.getPlayer().getUniqueId());
    }

    private boolean isSameLand(Land a, Land b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getId() == b.getId();
    }

    private void showEnterTitle(Player player, Land land) {
        player.sendTitle(
            TITLE,
            ChatColor.GREEN + landDisplayName(land) + "에 들어왔습니다.",
            5,
            40,
            10
        );
    }

    private void showExitTitle(Player player, Land land) {
        player.sendTitle(
            TITLE,
            ChatColor.GREEN + landDisplayName(land) + "에서 나갔습니다.",
            5,
            40,
            10
        );
    }

    private String landDisplayName(Land land) {
        if (land == null || !land.hasOwner() || land.getOwnerName() == null || land.getOwnerName().isBlank()) {
            return "빈 땅";
        }
        return land.getOwnerName() + "의 땅";
    }
}
