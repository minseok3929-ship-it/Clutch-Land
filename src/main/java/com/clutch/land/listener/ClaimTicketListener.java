package com.clutch.land.listener;

import com.clutch.land.infrastructure.AsyncDatabaseWriter;
import com.clutch.land.repository.LandRepository;
import com.clutch.land.service.LandCacheService;
import com.clutch.land.service.LandQueryService;
import com.clutch.land.ui.MessageFacade;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ClaimTicketListener implements Listener {
    public static final int CLAIM_TICKET_CUSTOM_MODEL_DATA = 991101;

    private final LandCacheService cacheService;
    private final LandQueryService landQueryService;
    private final LandRepository landRepository;
    private final AsyncDatabaseWriter writer;
    private final MessageFacade messageFacade;

    public ClaimTicketListener(
            LandCacheService cacheService,
            LandQueryService landQueryService,
            LandRepository landRepository,
            AsyncDatabaseWriter writer,
            MessageFacade messageFacade
    ) {
        this.cacheService = cacheService;
        this.landQueryService = landQueryService;
        this.landRepository = landRepository;
        this.writer = writer;
        this.messageFacade = messageFacade;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseTicket(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isClaimTicket(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        var landOpt = landQueryService.findAt(player);
        if (landOpt.isEmpty()) {
            messageFacade.error(player, "이 곳은 구매할 수 없는 땅 입니다.");
            event.setCancelled(true);
            return;
        }

        var land = landOpt.get();
        if (land.isOwned()) {
            messageFacade.error(player, "이미 소유자가 있는 토지입니다.");
            event.setCancelled(true);
            return;
        }

        String ownerUuid = player.getUniqueId().toString();
        cacheService.updateOwner(land.id(), ownerUuid);
        cacheService.clearMembers(land.id());
        writer.submit(() -> {
            landRepository.updateOwner(land.id(), ownerUuid);
        });

        consumeOne(event.getItem(), player);
        messageFacade.info(player, "토지를 구매(클레임)했습니다.");
        event.setCancelled(true);
    }

    private boolean isClaimTicket(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.hasCustomModelData()
                && meta.getCustomModelData() == CLAIM_TICKET_CUSTOM_MODEL_DATA
                && "땅 구매권".equals(meta.getDisplayName());
    }

    private void consumeOne(ItemStack item, Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        item.setAmount(Math.max(0, item.getAmount() - 1));
    }
}
