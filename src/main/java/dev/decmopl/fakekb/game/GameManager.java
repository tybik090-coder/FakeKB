package dev.decmopl.fakekb.game;

import dev.decmopl.fakekb.FakeKBPlugin;
import dev.decmopl.fakekb.util.Messages;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

    private final FakeKBPlugin plugin;
    private final BossBar gameBar;
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private GamePhase phase = GamePhase.IDLE;
    private World arena;
    private BukkitTask ticker;
    private int secondsLeft;
    private int cageSeconds;

    public GameManager(FakeKBPlugin plugin) {
        this.plugin = plugin;
        this.gameBar = BossBar.bossBar(
                plugin.messages().parse("FakeKB"),
                1f,
                plugin.messages().barColor(plugin.settings().bossBarGameColor(), BossBar.Color.RED),
                BossBar.Overlay.NOTCHED_10
        );
    }

    public boolean isRunning() {
        return phase.running();
    }

    public GamePhase phase() {
        return phase;
    }

    public boolean isParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public boolean isAlive(Player player) {
        return alive.contains(player.getUniqueId());
    }

    public World arena() {
        return arena;
    }

    public void start(List<Player> players, boolean forced) {
        if (isRunning()) {
            return;
        }
        List<Player> starters = new ArrayList<>(players);
        starters.removeIf(player -> !player.isOnline());
        if (starters.isEmpty()) {
            return;
        }
        if (forced) {
            starters.forEach(player -> plugin.messages().send(player, "messages.start-forced"));
        }
        phase = GamePhase.STARTING;
        plugin.lobbyManager().resetCountdown();
        participants.clear();
        alive.clear();
        starters.forEach(player -> {
            participants.add(player.getUniqueId());
            alive.add(player.getUniqueId());
        });

        String worldName = plugin.settings().worldNamePrefix() + System.currentTimeMillis();
        arena = plugin.worldService().createArenaWorld(worldName);
        if (arena == null) {
            failStart("messages.world-fail");
            return;
        }
        try {
            plugin.schematicService().pasteStartup(arena);
        } catch (Exception exception) {
            plugin.getLogger().warning("Paste startup: " + exception.getMessage());
            failStart("messages.schematic-fail", Map.of(
                    "name", plugin.settings().startupSchematic(),
                    "error", String.valueOf(exception.getMessage())
            ));
            return;
        }

        plugin.worldService().prepareBorder(arena, plugin.settings().borderStartSize());
        enterCages(Material.WHITE_STAINED_GLASS, plugin.settings().pasteX(), plugin.settings().pasteZ(),
                plugin.settings().startCagesRadius(), plugin.settings().startCagesYMin(), plugin.settings().startCagesYMax(),
                GamePhase.CAGE, "messages.phase-cage", "cage", "cage-sub");
        startTicker();
    }

    public void stop(boolean announce) {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        plugin.cageService().clearLocks();
        Location lobby = plugin.settings().lobbyLocation();
        for (UUID uuid : Set.copyOf(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(gameBar);
                player.setGameMode(GameMode.SURVIVAL);
                if (lobby != null) {
                    plugin.lobbyManager().teleportToLobby(player);
                }
            }
        }
        if (announce) {
            Bukkit.getOnlinePlayers().forEach(player -> plugin.messages().send(player, "messages.stop"));
        }
        World toDelete = arena;
        arena = null;
        participants.clear();
        alive.clear();
        phase = GamePhase.IDLE;
        secondsLeft = 0;
        plugin.lobbyManager().resetCountdown();
        if (toDelete != null) {
            plugin.worldService().deleteWorld(toDelete);
        }
    }

    public void eliminate(Player player, boolean broadcast) {
        if (!alive.remove(player.getUniqueId())) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        if (broadcast) {
            broadcast("messages.eliminated", Map.of(
                    "player", player.getName(),
                    "alive", String.valueOf(alive.size())
            ));
        }
        checkWin();
    }

    public void handleQuit(Player player) {
        if (!isParticipant(player)) {
            return;
        }
        eliminate(player, true);
        participants.remove(player.getUniqueId());
        player.hideBossBar(gameBar);
    }

    public void updateBossBar() {
        int total = Math.max(1, phaseDuration());
        float progress = Math.max(0f, Math.min(1f, secondsLeft / (float) total));
        gameBar.progress(progress);
        gameBar.color(plugin.messages().barColor(plugin.settings().bossBarGameColor(), BossBar.Color.RED));
        gameBar.name(plugin.messages().parse(Messages.apply(plugin.settings().bossBarGameTitle(), Map.of(
                "phase", phase.displayName(),
                "time", formatTime(secondsLeft),
                "alive", String.valueOf(alive.size())
        ))));
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showBossBar(gameBar);
            }
        }
    }

    private void startTicker() {
        if (ticker != null) {
            ticker.cancel();
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (!isRunning()) {
            return;
        }
        if (phase.cagesLocked()) {
            cageSeconds--;
            titleAlive("cage", "cage-sub", Map.of("seconds", String.valueOf(Math.max(cageSeconds, 0))));
            if (cageSeconds <= 0) {
                plugin.cageService().releaseAndBreak();
                if (phase == GamePhase.CAGE) {
                    beginPhase1();
                } else {
                    beginPhase4();
                }
            }
            secondsLeft = cageSeconds;
            updateBossBar();
            return;
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            nextPhase();
        }
        updateBossBar();
        checkWin();
    }

    private void nextPhase() {
        switch (phase) {
            case PHASE_1 -> beginPhase2();
            case PHASE_2 -> beginPhase3();
            case PHASE_3 -> beginFinaleCages();
            case PHASE_4 -> finish(null);
            default -> {
            }
        }
    }

    private void beginPhase1() {
        phase = GamePhase.PHASE_1;
        secondsLeft = plugin.settings().phase1Seconds();
        broadcast("messages.phase-1", Map.of());
        titleAlive("phase1", "phase1-sub", Map.of());
        forEachAlive(player -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvulnerable(false);
        });
    }

    private void beginPhase2() {
        phase = GamePhase.PHASE_2;
        secondsLeft = plugin.settings().phase2Seconds();
        broadcast("messages.phase-2", Map.of());
        titleAlive("phase2", "phase2-sub", Map.of());
        forEachAlive(player -> {
            KitType kit = plugin.kitService().get(player);
            plugin.kitService().give(player, kit);
            plugin.messages().send(player, "messages.kit-given", Map.of("kit", kit.displayName()));
        });
    }

    private void beginPhase3() {
        phase = GamePhase.PHASE_3;
        secondsLeft = plugin.settings().phase3Seconds();
        if (arena != null) {
            plugin.worldService().prepareBorder(arena, plugin.settings().borderStartSize());
            plugin.worldService().shrinkBorder(arena, plugin.settings().borderEndSize(), plugin.settings().phase3Seconds());
        }
        broadcast("messages.phase-3", Map.of());
        titleAlive("phase3", "phase3-sub", Map.of());
    }

    private void beginFinaleCages() {
        if (arena != null) {
            plugin.worldService().prepareBorder(arena, plugin.settings().borderPhase4Size());
        }
        enterCages(Material.GREEN_STAINED_GLASS, plugin.settings().phase4X(), plugin.settings().phase4Z(),
                plugin.settings().finaleCagesRadius(), plugin.settings().finaleCagesYMin(), plugin.settings().finaleCagesYMax(),
                GamePhase.PHASE_4_CAGE, "messages.phase-4-cage", "cage", "cage-sub");
    }

    private void beginPhase4() {
        phase = GamePhase.PHASE_4;
        secondsLeft = plugin.settings().phase4Seconds();
        broadcast("messages.phase-4", Map.of());
        titleAlive("phase4", "phase4-sub", Map.of());
        forEachAlive(player -> player.setGameMode(GameMode.SURVIVAL));
    }

    private void enterCages(Material glass, int cx, int cz, int radius, int yMin, int yMax,
                            GamePhase cagePhase, String messagePath, String title, String sub) {
        phase = cagePhase;
        cageSeconds = plugin.settings().cageCountdownSeconds();
        secondsLeft = cageSeconds;
        plugin.cageService().clearLocks();
        List<Location> pads = arena == null ? List.of() : plugin.cageService().findSpawns(arena, glass, cx, cz, radius, yMin, yMax);
        broadcast("messages.cages-found", Map.of("count", String.valueOf(pads.size())));
        int index = 0;
        List<Player> living = livingPlayers();
        for (Player player : living) {
            Location spawn;
            if (index < pads.size()) {
                spawn = pads.get(index);
            } else {
                double angle = (index * 2 * Math.PI) / Math.max(1, living.size());
                spawn = plugin.cageService().buildFallbackCage(
                        arena,
                        cx + Math.cos(angle) * 6,
                        plugin.settings().pasteY(),
                        cz + Math.sin(angle) * 6,
                        glass
                );
            }
            index++;
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            restoreHealth(player);
            player.setFoodLevel(20);
            player.teleport(spawn);
            plugin.cageService().lockPlayer(player, spawn, glass);
            player.showBossBar(gameBar);
        }
        broadcast(messagePath, Map.of("seconds", String.valueOf(cageSeconds)));
        titleAlive(title, sub, Map.of("seconds", String.valueOf(cageSeconds)));
    }

    private void restoreHealth(Player player) {
        var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
    }

    private void checkWin() {
        if (!isRunning() || phase == GamePhase.ENDING || phase == GamePhase.STARTING) {
            return;
        }
        if (alive.size() > 1) {
            return;
        }
        Player winner = alive.stream().map(Bukkit::getPlayer).filter(player -> player != null && player.isOnline()).findFirst().orElse(null);
        finish(winner);
    }

    private void finish(Player winner) {
        if (phase == GamePhase.ENDING) {
            return;
        }
        phase = GamePhase.ENDING;
        secondsLeft = plugin.settings().endDelaySeconds();
        if (winner != null) {
            broadcast("messages.winner", Map.of("player", winner.getName()));
            titleAlive("win", "win-sub", Map.of("player", winner.getName()));
        } else {
            broadcast("messages.no-winner", Map.of());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> stop(false), plugin.settings().endDelaySeconds() * 20L);
    }

    private void failStart(String path) {
        failStart(path, Map.of());
    }

    private void failStart(String path, Map<String, String> placeholders) {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.messages().send(player, path, placeholders);
            }
        }
        stop(false);
    }

    private void broadcast(String path, Map<String, String> placeholders) {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.messages().send(player, path, placeholders);
            }
        }
    }

    private void titleAlive(String title, String sub, Map<String, String> placeholders) {
        forEachAlive(player -> plugin.messages().title(player, title, sub, placeholders));
    }

    private void forEachAlive(java.util.function.Consumer<Player> consumer) {
        for (Player player : livingPlayers()) {
            consumer.accept(player);
        }
    }

    private List<Player> livingPlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                list.add(player);
            }
        }
        return list;
    }

    private int phaseDuration() {
        return switch (phase) {
            case CAGE, PHASE_4_CAGE -> plugin.settings().cageCountdownSeconds();
            case PHASE_1 -> plugin.settings().phase1Seconds();
            case PHASE_2 -> plugin.settings().phase2Seconds();
            case PHASE_3 -> plugin.settings().phase3Seconds();
            case PHASE_4 -> plugin.settings().phase4Seconds();
            case ENDING -> plugin.settings().endDelaySeconds();
            default -> 1;
        };
    }

    private String formatTime(int seconds) {
        int value = Math.max(seconds, 0);
        return String.format("%d:%02d", value / 60, value % 60);
    }
}
