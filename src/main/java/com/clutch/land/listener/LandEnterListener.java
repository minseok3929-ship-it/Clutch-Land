package com.clutch.land.listener;

import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LandEnterListener implements Listener {
    private final LandManager landManager;
    private final Map<UUID, Integer> lastLandId = new HashMap<>();

    public LandEnterListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
            && Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        Optional<Land> fromLand = landManager.getLandAt(event.getFrom());
        Optional<Land> toLand = landManager.getLandAt(event.getTo());

        Integer fromId = fromLand.map(Land::getId).orElse(null);
        Integer toId = toLand.map(Land::getId).orElse(null);

        Integer cached = lastLandId.get(player.getUniqueId());
        if (Objects.equals(cached, toId) || Objects.equals(fromId, toId)) {
            lastLandId.put(player.getUniqueId(), toId);
            return;
        }

        lastLandId.put(player.getUniqueId(), toId);
        if (toLand.isEmpty()) {
            return;
        }

        Land land = toLand.get();
        String title = land.hasOwner()
            ? ChatColor.GOLD + land.getOwnerName() + "의 땅"
            : ChatColor.GRAY + "<빈 땅>";
        player.sendTitle(title, "", 5, 40, 10);
    }
}
