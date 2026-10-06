package dev.decmopl.fakekb.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CageService {

    private static final BlockFace[] HORIZONTAL = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final Map<UUID, BoundingBox> locked = new ConcurrentHashMap<>();
    private final Set<Location> glassToBreak = ConcurrentHashMap.newKeySet();

    public List<Location> findSpawns(World world, Material glass, int cx, int cz, int radius, int yMin, int yMax) {
        List<Location> spawns = new ArrayList<>();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    Block floor = world.getBlockAt(x, y, z);
                    if (isSpawnPad(floor, glass)) {
                        spawns.add(floor.getLocation().add(0.5, 1.01, 0.5));
                    }
                }
            }
        }
        return spawns;
    }

    public boolean isSpawnPad(Block floor, Material glass) {
        if (floor.getType() != glass) {
            return false;
        }
        Block standing = floor.getRelative(BlockFace.UP);
        if (!standing.isPassable()) {
            return false;
        }
        for (BlockFace face : HORIZONTAL) {
            if (standing.getRelative(face).getType() != glass) {
                return false;
            }
        }
        return true;
    }

    public Location buildFallbackCage(World world, double x, double y, double z, Material glass) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setGlass(world, bx + dx, by, bz + dz, glass);
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            for (BlockFace face : HORIZONTAL) {
                setGlass(world, bx + face.getModX(), by + dy, bz + face.getModZ(), glass);
            }
            setGlass(world, bx, by + 3, bz, glass);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setGlass(world, bx + dx, by + 3, bz + dz, glass);
            }
        }
        return new Location(world, bx + 0.5, by + 1.01, bz + 0.5);
    }

    public void lockPlayer(Player player, Location spawn, Material glass) {
        World world = spawn.getWorld();
        int x = spawn.getBlockX();
        int y = spawn.getBlockY() - 1;
        int z = spawn.getBlockZ();
        Set<Location> blocks = new HashSet<>();
        collectGlass(world, x, y, z, glass, blocks);
        for (int dy = 1; dy <= 3; dy++) {
            collectGlass(world, x, y + dy, z, glass, blocks);
            for (BlockFace face : HORIZONTAL) {
                collectGlass(world, x + face.getModX(), y + dy, z + face.getModZ(), glass, blocks);
            }
        }
        glassToBreak.addAll(blocks);
        locked.put(player.getUniqueId(), BoundingBox.of(
                spawn.clone().add(-0.85, -0.2, -0.85),
                spawn.clone().add(0.85, 2.6, 0.85)
        ));
    }

    public boolean isLocked(Player player) {
        return locked.containsKey(player.getUniqueId());
    }

    public boolean outsideCage(Player player, Location to) {
        BoundingBox box = locked.get(player.getUniqueId());
        return box != null && !box.contains(to.getX(), to.getY(), to.getZ());
    }

    public Location cageCenter(Player player) {
        BoundingBox box = locked.get(player.getUniqueId());
        if (box == null || player.getWorld() == null) {
            return player.getLocation();
        }
        return new Location(player.getWorld(), box.getCenterX(), box.getMinY() + 0.2, box.getCenterZ());
    }

    public void releaseAndBreak() {
        for (Location location : glassToBreak) {
            Block block = location.getBlock();
            if (block.getType().name().contains("GLASS")) {
                block.setType(Material.AIR, false);
            }
        }
        glassToBreak.clear();
        locked.clear();
    }

    public void clearLocks() {
        glassToBreak.clear();
        locked.clear();
    }

    private void collectGlass(World world, int x, int y, int z, Material glass, Set<Location> into) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == glass) {
            into.add(block.getLocation());
        }
    }

    private void setGlass(World world, int x, int y, int z, Material glass) {
        world.getBlockAt(x, y, z).setType(glass, false);
    }
}
