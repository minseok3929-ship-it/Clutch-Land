package com.clutch.land.command;

import com.clutch.land.ClutchLandPlugin;
import com.clutch.land.manager.LandManager;
import com.clutch.land.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LandPlayerCommand implements CommandExecutor {
    private final LandManager landManager;

    public LandPlayerCommand(LandManager landManager) {
        this.landManager = landManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ClutchLandPlugin.PREFIX + "플레이어만 사용 가능합니다.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /땅 <공유|공유해제|양도|삭제>");
            return true;
        }

        switch (args[0]) {
            case "공유" -> share(player, args);
            case "공유해제" -> unshare(player, args);
            case "양도" -> transfer(player, args);
            case "삭제" -> removeOwnership(player);
            default -> player.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /땅 <공유|공유해제|양도|삭제>");
        }
        return true;
    }

    private Optional<Land> getOwnedLandOrNotify(Player player) {
        Optional<Land> land = landManager.getOwnedLand(player.getUniqueId());
        if (land.isEmpty()) {
            player.sendMessage(ClutchLandPlugin.PREFIX + "본인이 소유한 땅이 없습니다.");
        }
        return land;
    }

    private void share(Player owner, String[] args) {
        if (args.length < 2) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /땅 공유 <닉네임>");
            return;
        }
        Optional<Land> optLand = getOwnedLandOrNotify(owner);
        if (optLand.isEmpty()) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        boolean ok = landManager.addMember(optLand.get(), target);
        if (!ok) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "공유에 실패했습니다.");
            return;
        }

        owner.sendMessage(ClutchLandPlugin.PREFIX + "땅 권한을 공유했습니다!");
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "땅 권한을 공유했습니다!");
        }
    }

    private void unshare(Player owner, String[] args) {
        if (args.length < 2) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /땅 공유해제 <닉네임>");
            return;
        }
        Optional<Land> optLand = getOwnedLandOrNotify(owner);
        if (optLand.isEmpty()) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        boolean ok = landManager.removeMember(optLand.get(), target);
        if (!ok) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "공유해제에 실패했습니다.");
            return;
        }

        owner.sendMessage(ClutchLandPlugin.PREFIX + "땅 권한을 공유해제했습니다!");
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "땅 권한을 공유해제했습니다!");
        }
    }

    private void transfer(Player owner, String[] args) {
        if (args.length < 2) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "사용법: /땅 양도 <닉네임>");
            return;
        }
        Optional<Land> optLand = getOwnedLandOrNotify(owner);
        if (optLand.isEmpty()) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        boolean ok = landManager.transferLand(optLand.get(), target);
        if (!ok) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "땅을 양도받을 수 없는 상태입니다!");
            return;
        }

        owner.sendMessage(ClutchLandPlugin.PREFIX + "땅을 양도했습니다!");
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ClutchLandPlugin.PREFIX + "땅을 양도받았습니다!");
        }
    }

    private void removeOwnership(Player owner) {
        Optional<Land> optLand = getOwnedLandOrNotify(owner);
        if (optLand.isEmpty()) return;

        if (landManager.unclaimLand(optLand.get())) {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "땅을 삭제하였습니다.");
        } else {
            owner.sendMessage(ClutchLandPlugin.PREFIX + "땅 삭제에 실패했습니다.");
        }
    }
}
