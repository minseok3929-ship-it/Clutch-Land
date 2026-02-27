package com.clutch.land.command;

import com.clutch.land.infrastructure.AsyncDatabaseWriter;
import com.clutch.land.repository.LandMemberRepository;
import com.clutch.land.repository.LandRepository;
import com.clutch.land.service.LandCacheService;
import com.clutch.land.service.LandQueryService;
import com.clutch.land.ui.MessageFacade;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class PlayerLandCommand implements CommandExecutor, TabCompleter {
    private final LandCacheService cacheService;
    private final LandQueryService landQueryService;
    private final LandRepository landRepository;
    private final LandMemberRepository landMemberRepository;
    private final AsyncDatabaseWriter writer;
    private final MessageFacade messageFacade;

    public PlayerLandCommand(
            LandCacheService cacheService,
            LandQueryService landQueryService,
            LandRepository landRepository,
            LandMemberRepository landMemberRepository,
            AsyncDatabaseWriter writer,
            MessageFacade messageFacade
    ) {
        this.cacheService = cacheService;
        this.landQueryService = landQueryService;
        this.landRepository = landRepository;
        this.landMemberRepository = landMemberRepository;
        this.writer = writer;
        this.messageFacade = messageFacade;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messageFacade.error(sender, "플레이어만 사용할 수 있습니다.");
            return true;
        }

        var landOpt = landQueryService.findAt(player);
        if (landOpt.isEmpty()) {
            messageFacade.error(player, "현재 위치는 등록 토지가 아닙니다.");
            return true;
        }
        var land = landOpt.get();

        if (!cacheService.isOwner(land.id(), player.getUniqueId())) {
            messageFacade.error(player, "토지 소유자만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            messageFacade.warn(player, "사용법: /땅 <공유|공유해제|양도|삭제>");
            return true;
        }

        switch (args[0]) {
            case "삭제" -> unclaim(player, land.id());
            case "양도" -> transfer(player, land.id(), args);
            case "공유" -> share(player, land.id(), args);
            case "공유해제" -> unshare(player, land.id(), args);
            default -> messageFacade.warn(player, "알 수 없는 하위 명령어입니다.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("공유", "공유해제", "양도", "삭제").stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        }
        return List.of();
    }

    private void share(Player player, int landId, String[] args) {
        if (args.length < 2) {
            messageFacade.warn(player, "사용법: /땅 공유 <닉네임>");
            return;
        }
        UUID targetUuid = resolvePlayerUuid(args[1]);
        if (targetUuid == null) {
            messageFacade.error(player, "대상을 찾을 수 없습니다.");
            return;
        }

        cacheService.addMember(landId, targetUuid);
        writer.submit(() -> landMemberRepository.addMember(landId, targetUuid, "MEMBER"));
        messageFacade.info(player, "공유 대상을 추가했습니다.");
    }

    private void unshare(Player player, int landId, String[] args) {
        if (args.length < 2) {
            messageFacade.warn(player, "사용법: /땅 공유해제 <닉네임>");
            return;
        }
        UUID targetUuid = resolvePlayerUuid(args[1]);
        if (targetUuid == null) {
            messageFacade.error(player, "대상을 찾을 수 없습니다.");
            return;
        }

        cacheService.removeMember(landId, targetUuid);
        writer.submit(() -> landMemberRepository.removeMember(landId, targetUuid));
        messageFacade.info(player, "공유를 해제했습니다.");
    }

    private void transfer(Player player, int landId, String[] args) {
        if (args.length < 2) {
            messageFacade.warn(player, "사용법: /땅 양도 <닉네임>");
            return;
        }
        UUID targetUuid = resolvePlayerUuid(args[1]);
        if (targetUuid == null) {
            messageFacade.error(player, "대상을 찾을 수 없습니다.");
            return;
        }

        cacheService.updateOwner(landId, targetUuid.toString());
        cacheService.clearMembers(landId);
        writer.submit(() -> {
            landRepository.updateOwner(landId, targetUuid.toString());
            landMemberRepository.removeAllByLand(landId);
        });
        messageFacade.info(player, "토지를 양도했습니다.");
    }

    private void unclaim(Player player, int landId) {
        cacheService.updateOwner(landId, null);
        cacheService.clearMembers(landId);
        writer.submit(() -> {
            landRepository.updateOwner(landId, null);
            landMemberRepository.removeAllByLand(landId);
        });
        messageFacade.info(player, "토지를 삭제(언클레임)했습니다.");
    }

    private UUID resolvePlayerUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline == null || offline.getUniqueId() == null) {
            return null;
        }
        return offline.getUniqueId();
    }
}
