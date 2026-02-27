package com.clutch.land.command;

import com.clutch.land.domain.ChunkKey;
import com.clutch.land.domain.Land;
import com.clutch.land.listener.SelectionToolListener;
import com.clutch.land.repository.LandChunkRepository;
import com.clutch.land.repository.LandRepository;
import com.clutch.land.service.LandCacheService;
import com.clutch.land.service.SelectionService;
import com.clutch.land.ui.MessageFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class LandRootCommand implements CommandExecutor, TabCompleter {
    private final SelectionService selectionService;
    private final LandRepository landRepository;
    private final LandChunkRepository landChunkRepository;
    private final LandCacheService landCacheService;
    private final MessageFacade messageFacade;

    public LandRootCommand(
            SelectionService selectionService,
            LandRepository landRepository,
            LandChunkRepository landChunkRepository,
            LandCacheService landCacheService,
            MessageFacade messageFacade
    ) {
        this.selectionService = selectionService;
        this.landRepository = landRepository;
        this.landChunkRepository = landChunkRepository;
        this.landCacheService = landCacheService;
        this.messageFacade = messageFacade;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messageFacade.error(sender, "플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.isOp()) {
            messageFacade.error(player, "관리자만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            messageFacade.warn(player, "사용법: /토지 <도구|생성>");
            return true;
        }

        String sub = args[0];
        if ("도구".equalsIgnoreCase(sub)) {
            giveTool(player);
            return true;
        }

        if ("생성".equalsIgnoreCase(sub)) {
            createLand(player);
            return true;
        }

        messageFacade.warn(player, "알 수 없는 하위 명령어입니다.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("도구", "생성").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void giveTool(Player player) {
        ItemStack tool = new ItemStack(Material.COMPASS);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6토지 선택 나침반");
            meta.setCustomModelData(SelectionToolListener.TOOL_CUSTOM_MODEL_DATA);
            List<String> lore = new ArrayList<>();
            lore.add("§7좌클릭: 1번 지점");
            lore.add("§7우클릭: 2번 지점");
            meta.setLore(lore);
            tool.setItemMeta(meta);
        }
        player.getInventory().addItem(tool);
        messageFacade.info(player, "토지 선택 도구를 지급했습니다.");
    }

    private void createLand(Player player) {
        Optional<SelectionService.Selection> selectionOpt = selectionService.get(player.getUniqueId());
        if (selectionOpt.isEmpty() || !selectionOpt.get().isComplete()) {
            messageFacade.error(player, "선택이 완료되지 않았습니다. 두 지점을 먼저 선택하세요.");
            return;
        }

        SelectionService.SelectionBounds bounds = selectionOpt.get().toBounds();

        Set<Integer> candidates = landCacheService.collectCandidateLandIds(
                bounds.worldId(),
                bounds.minX(),
                bounds.maxX(),
                bounds.minZ(),
                bounds.maxZ()
        );

        for (int candidateId : candidates) {
            Optional<Land> existing = landCacheService.getLandById(candidateId);
            if (existing.isEmpty()) {
                continue;
            }
            Land old = existing.get();
            if (!old.worldId().equals(bounds.worldId())) {
                continue;
            }
            if (isOverlapped2D(bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ(),
                    old.minX(), old.maxX(), old.minZ(), old.maxZ())) {
                messageFacade.error(player, "기존 토지와 겹쳐 생성할 수 없습니다. (ID: " + old.id() + ")");
                return;
            }
        }

        Land newLand = new Land(
                0,
                bounds.worldId(),
                bounds.minX(),
                bounds.maxX(),
                bounds.minY(),
                bounds.maxY(),
                bounds.minZ(),
                bounds.maxZ(),
                null,
                0,
                "{}"
        );

        int newLandId = landRepository.insert(newLand);
        Set<ChunkKey> chunks = computeChunks(bounds.worldId(), bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ());
        landChunkRepository.replaceLandChunks(newLandId, chunks);

        Land persisted = new Land(
                newLandId,
                newLand.worldId(),
                newLand.minX(),
                newLand.maxX(),
                newLand.minY(),
                newLand.maxY(),
                newLand.minZ(),
                newLand.maxZ(),
                newLand.ownerUuid(),
                newLand.grade(),
                newLand.flagsJson()
        );
        landCacheService.addLand(persisted, chunks);

        messageFacade.info(player, "토지를 생성했습니다. ID=" + newLandId + ", 청크=" + chunks.size());
    }

    private boolean isOverlapped2D(
            int minX1, int maxX1, int minZ1, int maxZ1,
            int minX2, int maxX2, int minZ2, int maxZ2
    ) {
        return !(maxX1 < minX2 || minX1 > maxX2 || maxZ1 < minZ2 || minZ1 > maxZ2);
    }

    private Set<ChunkKey> computeChunks(String worldId, int minX, int maxX, int minZ, int maxZ) {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        Set<ChunkKey> chunks = new java.util.HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunks.add(new ChunkKey(worldId, cx, cz));
            }
        }
        return chunks;
    }
}
