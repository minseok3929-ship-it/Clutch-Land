package com.clutch.land.command;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.item.LandItemFactory;
import com.clutch.land.manager.LandManager;
import com.clutch.land.manager.SelectionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LandAdminCommand implements CommandExecutor {
    private final LandManager landManager;
    private final SelectionManager selectionManager;
    private final LandItemFactory itemFactory;

    public LandAdminCommand(LandManager landManager, SelectionManager selectionManager, LandItemFactory itemFactory) {
        this.landManager = landManager;
        this.selectionManager = selectionManager;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ClutchLandPlugin.PREFIX + "플레이어만 사용 가능합니다.");
            return true;
        }
        if (!player.isOp() && !player.hasPermission(ClutchLandPlugin.ADMIN_PERMISSION)) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "권한이 없습니다.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /토지 <도구|생성|삭제|해임|구매권>");
            return true;
        }

        switch (args[0]) {
            case "도구" -> {
                itemFactory.giveLandTool(player);
                player.sendMessage(ClutchLandPlugin.PREFIX + "토지 도구를 지급했습니다.");
            }
            case "생성" -> handleCreate(player);
            case "삭제" -> handleDelete(player);
            case "해임" -> handleDismiss(player, args);
            case "구매권" -> {
                player.getInventory().addItem(itemFactory.createClaimTicket());
                player.sendMessage(ClutchLandPlugin.PREFIX + "땅 구매권을 지급했습니다.");
            }
            default -> player.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /토지 <도구|생성|삭제|해임|구매권>");
        }
        return true;
    }

    private void handleCreate(Player player) {
        Location pos1 = selectionManager.getFirst(player.getUniqueId());
        Location pos2 = selectionManager.getSecond(player.getUniqueId());
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "먼저 두 지점을 설정하세요.");
            return;
        }
        if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "같은 월드에서만 생성할 수 있습니다.");
            return;
        }

        boolean created = landManager.createLand(
            pos1.getWorld().getName(),
            pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
            pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()
        );

        if (created) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "토지를 생성했습니다.");
        } else {
            player.sendMessage(ClutchLandPlugin.PREFIX + "이미 다른 토지와 겹칩니다.");
        }
    }

    private void handleDelete(Player player) {
        Location pos1 = selectionManager.getFirst(player.getUniqueId());
        Location pos2 = selectionManager.getSecond(player.getUniqueId());
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "먼저 두 지점을 설정하세요.");
            return;
        }
        if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "같은 월드에서만 삭제할 수 있습니다.");
            return;
        }

        int deleted = landManager.deleteLandRegion(
            pos1.getWorld().getName(),
            pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
            pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()
        );

        if (deleted > 0) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "토지를 삭제했습니다.");
        } else {
            player.sendMessage(ClutchLandPlugin.PREFIX + "삭제할 토지가 없습니다.");
        }
    }

    private void handleDismiss(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /토지 해임 <닉네임>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        boolean ok = landManager.dismissOwnedLandByPlayer(target);
        if (ok) {
            player.sendMessage(ClutchLandPlugin.PREFIX + args[1] + "님의 토지를 해임했습니다.");
        } else {
            player.sendMessage(ClutchLandPlugin.PREFIX + "해당 플레이어의 토지를 찾을 수 없습니다.");
        }
    }
}
