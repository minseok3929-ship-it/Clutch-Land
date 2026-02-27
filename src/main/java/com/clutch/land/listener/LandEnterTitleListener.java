package com.clutch.land.listener;

import com.clutch.land.domain.Land;
import com.clutch.land.service.LandQueryService;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class LandEnterTitleListener implements Listener {
    private final LandQueryService landQueryService;

    public LandEnterTitleListener(LandQueryService landQueryService) {
        this.landQueryService = landQueryService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return;
        }

        boolean sameBlock = event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getWorld().equals(event.getTo().getWorld());

        boolean sameChunk = (event.getFrom().getBlockX() >> 4) == (event.getTo().getBlockX() >> 4)
                && (event.getFrom().getBlockZ() >> 4) == (event.getTo().getBlockZ() >> 4)
                && event.getFrom().getWorld().equals(event.getTo().getWorld());

        if (sameBlock || sameChunk) {
            return;
        }

        Player player = event.getPlayer();
        Integer before = landQueryService.getCurrentLandId(player.getUniqueId());
        var nowLand = landQueryService.findAt(event.getTo());
        Integer now = nowLand.map(Land::id).orElse(null);

        if ((before == null && now == null) || (before != null && before.equals(now))) {
            return;
        }

        landQueryService.setCurrentLandId(player.getUniqueId(), now);

        if (nowLand.isPresent()) {
            player.sendTitle("", buildTitle(nowLand.get()), 5, 40, 10);
        }
    }

    private String buildTitle(Land land) {
        if (!land.isOwned()) {
            return "<빈 땅>";
        }

        try {
            UUID ownerUuid = UUID.fromString(land.ownerUuid());
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            String ownerName = owner.getName();
            if (ownerName == null || ownerName.isBlank()) {
                ownerName = ownerUuid.toString().substring(0, 8);
            }
            return ownerName + "의 땅";
        } catch (IllegalArgumentException e) {
            return "알 수 없는 소유자의 땅";
        }
    }
}
