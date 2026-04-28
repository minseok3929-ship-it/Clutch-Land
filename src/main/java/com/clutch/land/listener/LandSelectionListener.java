package com.clutch.land.listener;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.item.LandItemFactory;
import com.clutch.land.manager.SelectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LandSelectionListener implements Listener {
    private final SelectionManager selectionManager;
    private final LandItemFactory itemFactory;

    public LandSelectionListener(SelectionManager selectionManager, LandItemFactory itemFactory) {
        this.selectionManager = selectionManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSelect(PlayerInteractEvent event) {
        if (event.getItem() == null || !itemFactory.isTool(event.getItem())) {
            return;
        }
        if (event.getPlayer() == null) {
            return;
        }
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission(ClutchLandPlugin.ADMIN_PERMISSION)) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setFirst(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "첫 번째 지점이 설정되었습니다.");
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setSecond(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "두 번째 지점이 설정되었습니다.");
            event.setCancelled(true);
        }
    }
}
