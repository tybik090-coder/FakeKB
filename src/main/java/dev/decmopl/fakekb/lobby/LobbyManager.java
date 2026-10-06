package dev.decmopl.fakekb.lobby;

import dev.decmopl.fakekb.FakeKBPlugin;
import dev.decmopl.fakekb.util.Messages;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LobbyManager {

    private final FakeKBPlugin plugin;
    private final BossBar bossBar;
    private BukkitTask task;
    private int countdown = -1;

    public LobbyManager(FakeKBPlugin plugin) {
        this.plugin = plugin;
        this.bossBar = BossBar.bossBar(
                plugin.messages().parse("FakeKB"),
                1f,
                plugin.messages().barColor(plugin.settings().bossBarLobbyColor(), BossBar.Color.PINK),
                BossBar.Overlay.PROGRESS
        );
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        hideFromAll();
        countdown = -1;
    }

    public void refreshBossBar() {
        bossBar.color(plugin.messages().barColor(plugin.settings().bossBarLobbyColor(), BossBar.Color.PINK));
    }

    public List<Player> playersInZone() {
        Location lobby = plugin.settings().lobbyLocation();
        if (lobby == null || lobby.getWorld() == null) {
            return List.of();
        }
        double radius = plugin.settings().lobbyRadius();
        double radiusSq = radius * radius;
        List<Player> found = new ArrayList<>();
        for (Player player : lobby.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(lobby) <= radiusSq) {
                found.add(player);
            }
        }
        return found;
    }

    public void teleportToLobby(Player player) {
        Location lobby = plugin.settings().lobbyLocation();
        if (lobby == null) {
            return;
        }
        player.teleport(lobby);
        player.setGameMode(GameMode.ADVENTURE);
        var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        plugin.kitMenu().giveSelector(player);
        plugin.messages().send(player, "messages.teleport-lobby");
    }

    public void resetCountdown() {
        countdown = -1;
    }

    private void tick() {
        if (plugin.gameManager().isRunning()) {
            plugin.gameManager().updateBossBar();
        }
        Location lobby = plugin.settings().lobbyLocation();
        if (lobby == null) {
            hideFromAll();
            return;
        }
        List<Player> inZone = playersInZone();
        int count = inZone.size();
        int min = plugin.settings().minPlayers();
        showToWorld(lobby);

        if (!plugin.gameManager().isRunning()) {
            if (countdown >= 0) {
                if (count < min) {
                    countdown = -1;
                    broadcastZone(inZone, "messages.countdown-cancelled", Map.of("min", String.valueOf(min)));
                } else if (countdown == 0) {
                    countdown = -1;
                    plugin.gameManager().start(inZone, false);
                    return;
                } else {
                    if (countdown <= 5 || countdown % 5 == 0) {
                        broadcastZone(inZone, "messages.countdown-tick", Map.of("seconds", String.valueOf(countdown)));
                    }
                    countdown--;
                }
            } else if (count >= min) {
                countdown = plugin.settings().lobbyCountdownSeconds();
            }
        }

        float progress;
        String template;
        if (countdown >= 0) {
            int total = Math.max(1, plugin.settings().lobbyCountdownSeconds());
            progress = Math.max(0f, Math.min(1f, (countdown + 1f) / total));
            template = plugin.settings().bossBarCountdownTitle();
        } else {
            progress = Math.max(0f, Math.min(1f, count / (float) Math.max(1, min)));
            template = plugin.settings().bossBarLobbyTitle();
        }
        bossBar.progress(progress);
        bossBar.color(plugin.messages().barColor(plugin.settings().bossBarLobbyColor(), BossBar.Color.PINK));
        bossBar.name(plugin.messages().parse(Messages.apply(template, Map.of(
                "count", String.valueOf(count),
                "min", String.valueOf(min),
                "seconds", String.valueOf(Math.max(countdown, 0))
        ))));
    }

    private void broadcastZone(List<Player> players, String path, Map<String, String> placeholders) {
        for (Player player : players) {
            plugin.messages().send(player, path, placeholders);
        }
    }

    private void showToWorld(Location lobby) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.gameManager().isParticipant(player)) {
                player.hideBossBar(bossBar);
            } else if (player.getWorld().equals(lobby.getWorld())) {
                player.showBossBar(bossBar);
            } else {
                player.hideBossBar(bossBar);
            }
        }
    }

    public void hideFromAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    public BossBar bossBar() {
        return bossBar;
    }
}
