package dev.decmopl.fakekb.config;

import dev.decmopl.fakekb.FakeKBPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {

    private final FakeKBPlugin plugin;

    public PluginSettings(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public int minPlayers() {
        return cfg().getInt("settings.min-players", 5);
    }

    public int lobbyRadius() {
        return cfg().getInt("settings.lobby-radius", 25);
    }

    public void lobbyRadius(int radius) {
        cfg().set("settings.lobby-radius", Math.max(1, radius));
        plugin.saveConfig();
    }

    public int lobbyCountdownSeconds() {
        return cfg().getInt("settings.lobby-countdown-seconds", 15);
    }

    public int cageCountdownSeconds() {
        return cfg().getInt("settings.cage-countdown-seconds", 30);
    }

    public String worldNamePrefix() {
        return cfg().getString("settings.world-name-prefix", "fakekb_");
    }

    public String startupSchematic() {
        return cfg().getString("settings.startup-schematic", "startup");
    }

    public int pasteX() {
        return cfg().getInt("settings.schematic-paste.x", 0);
    }

    public int pasteY() {
        return cfg().getInt("settings.schematic-paste.y", 100);
    }

    public int pasteZ() {
        return cfg().getInt("settings.schematic-paste.z", 0);
    }

    public int phase4X() {
        return cfg().getInt("settings.phase4-teleport.x", 0);
    }

    public int phase4Z() {
        return cfg().getInt("settings.phase4-teleport.z", 0);
    }

    public double borderCenterX() {
        return cfg().getDouble("settings.world-border.center-x", 0.0);
    }

    public double borderCenterZ() {
        return cfg().getDouble("settings.world-border.center-z", 0.0);
    }

    public double borderStartSize() {
        return cfg().getDouble("settings.world-border.start-size", 1500.0);
    }

    public double borderEndSize() {
        return cfg().getDouble("settings.world-border.end-size", 50.0);
    }

    public double borderPhase4Size() {
        return cfg().getDouble("settings.world-border.phase4-size", 80.0);
    }

    public int startCagesRadius() {
        return cfg().getInt("settings.scan.start-cages-radius", 80);
    }

    public int startCagesYMin() {
        return cfg().getInt("settings.scan.start-cages-y-min", 80);
    }

    public int startCagesYMax() {
        return cfg().getInt("settings.scan.start-cages-y-max", 140);
    }

    public int finaleCagesRadius() {
        return cfg().getInt("settings.scan.finale-cages-radius", 64);
    }

    public int finaleCagesYMin() {
        return cfg().getInt("settings.scan.finale-cages-y-min", 1);
    }

    public int finaleCagesYMax() {
        return cfg().getInt("settings.scan.finale-cages-y-max", 320);
    }

    public int phase1Seconds() {
        return cfg().getInt("phases.phase1-seconds", 180);
    }

    public int phase2Seconds() {
        return cfg().getInt("phases.phase2-seconds", 1200);
    }

    public int phase3Seconds() {
        return cfg().getInt("phases.phase3-seconds", 900);
    }

    public int phase4Seconds() {
        return cfg().getInt("phases.phase4-seconds", 600);
    }

    public int endDelaySeconds() {
        return cfg().getInt("phases.end-delay-seconds", 8);
    }

    public boolean lobbySet() {
        return cfg().getBoolean("lobby.set", false);
    }

    public Location lobbyLocation() {
        if (!lobbySet()) {
            return null;
        }
        World world = Bukkit.getWorld(cfg().getString("lobby.world", ""));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                cfg().getDouble("lobby.x"),
                cfg().getDouble("lobby.y"),
                cfg().getDouble("lobby.z"),
                (float) cfg().getDouble("lobby.yaw"),
                (float) cfg().getDouble("lobby.pitch")
        );
    }

    public void saveLobby(Location location) {
        cfg().set("lobby.world", location.getWorld().getName());
        cfg().set("lobby.x", location.getX());
        cfg().set("lobby.y", location.getY());
        cfg().set("lobby.z", location.getZ());
        cfg().set("lobby.yaw", location.getYaw());
        cfg().set("lobby.pitch", location.getPitch());
        cfg().set("lobby.set", true);
        plugin.saveConfig();
    }

    public String bossBarLobbyTitle() {
        return cfg().getString("bossbar.lobby-title", "игроков: {count}");
    }

    public String bossBarCountdownTitle() {
        return cfg().getString("bossbar.countdown-title", "старт {seconds}");
    }

    public String bossBarGameTitle() {
        return cfg().getString("bossbar.game-title", "{phase} {time}");
    }

    public String bossBarLobbyColor() {
        return cfg().getString("bossbar.lobby-color", "PINK");
    }

    public String bossBarGameColor() {
        return cfg().getString("bossbar.game-color", "RED");
    }
}
