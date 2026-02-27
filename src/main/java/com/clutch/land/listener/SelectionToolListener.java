package com.clutch.land.listener;

import com.clutch.land.service.SelectionService;
import com.clutch.land.ui.MessageFacade;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SelectionToolListener implements Listener {
    public static final int TOOL_CUSTOM_MODEL_DATA = 991001;

    private final SelectionService selectionService;
    private final MessageFacade messageFacade;

    public SelectionToolListener(SelectionService selectionService, MessageFacade messageFacade) {
        this.selectionService = selectionService;
        this.messageFacade = messageFacade;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (!isSelectionTool(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionService.setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
            messageFacade.info(player, "1번 지점을 저장했습니다.");
            event.setCancelled(true);
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            selectionService.setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
            messageFacade.info(player, "2번 지점을 저장했습니다.");
            event.setCancelled(true);
        }
    }

    private boolean isSelectionTool(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == TOOL_CUSTOM_MODEL_DATA;
    }
}
