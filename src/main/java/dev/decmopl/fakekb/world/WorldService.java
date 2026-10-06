package dev.decmopl.fakekb.world;

import dev.decmopl.fakekb.FakeKBPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class WorldService {

    private final FakeKBPlugin plugin;

    public WorldService(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    public World createArenaWorld(String name) {
        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(false)
                .generator(new VoidChunkGenerator());
        World world = creator.createWorld();
        if (world == null) {
            return null;
        }
        world.setSpawnLocation(plugin.settings().pasteX(), plugin.settings().pasteY(), plugin.settings().pasteZ());
        world.setAutoSave(false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setTime(6000);
        importToMultiverse(name);
        loadChunks(world, plugin.settings().pasteX() >> 4, plugin.settings().pasteZ() >> 4, 8);
        return world;
    }

    public void prepareBorder(World world, double size) {
        var border = world.getWorldBorder();
        border.setCenter(plugin.settings().borderCenterX(), plugin.settings().borderCenterZ());
        border.setDamageAmount(1.0);
        border.setDamageBuffer(2.0);
        border.setWarningDistance(15);
        border.setSize(size);
    }

    public void shrinkBorder(World world, double toSize, long seconds) {
        world.getWorldBorder().setSize(toSize, seconds);
    }

    public void deleteWorld(World world) {
        if (world == null) {
            return;
        }
        String name = world.getName();
        Bukkit.unloadWorld(world, false);
        removeFromMultiverse(name);
        Path folder = Bukkit.getWorldContainer().toPath().resolve(name);
        try {
            if (Files.exists(folder)) {
                Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось удалить мир " + name + ": " + exception.getMessage());
        }
    }

    private void importToMultiverse(String worldName) {
        if (!hasMultiverse()) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv import " + worldName + " normal");
    }

    private void removeFromMultiverse(String worldName) {
        if (!hasMultiverse()) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + worldName);
    }

    private boolean hasMultiverse() {
        Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        return mv != null && mv.isEnabled();
    }

    private void loadChunks(World world, int cx, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                world.getChunkAt(x, z).load(true);
            }
        }
    }
}
