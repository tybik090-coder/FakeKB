package dev.decmopl.fakekb.game;

import dev.decmopl.fakekb.FakeKBPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class GameListener implements Listener {

    private final FakeKBPlugin plugin;

    public GameListener(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.settings().lobbySet() && plugin.settings().lobbyLocation() != null
                && player.getWorld().equals(plugin.settings().lobbyLocation().getWorld())
                && !plugin.gameManager().isRunning()) {
            plugin.kitMenu().giveSelector(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.gameManager().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.gameManager().phase().cagesLocked()) {
            return;
        }
        if (!plugin.cageService().isLocked(player)) {
            return;
        }
        if (plugin.cageService().outsideCage(player, event.getTo())) {
            event.setTo(plugin.cageService().cageCenter(player));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!plugin.gameManager().isAlive(player)) {
            return;
        }
        event.getDrops().clear();
        plugin.gameManager().eliminate(player, true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.gameManager().isParticipant(player)) {
            return;
        }
        if (plugin.gameManager().arena() != null) {
            event.setRespawnLocation(plugin.gameManager().arena().getSpawnLocation());
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!plugin.gameManager().isParticipant(victim)) {
            return;
        }
        if (!plugin.gameManager().phase().pvp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.gameManager().phase().cagesLocked() && plugin.gameManager().isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.gameManager().phase().cagesLocked() && plugin.gameManager().isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
