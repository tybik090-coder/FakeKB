package dev.decmopl.fakekb.world;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.decmopl.fakekb.FakeKBPlugin;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

public final class SchematicService {

    public record CheckResult(boolean ok, String name, String format, String size, String error) {
        public static CheckResult fail(String name, String error) {
            return new CheckResult(false, name, "-", "-", error);
        }
    }

    private final FakeKBPlugin plugin;

    public SchematicService(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean worldEditPresent() {
        return plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null
                || plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    public File resolveFile(String name) {
        File folder = plugin.getSchemFolder();
        String trimmed = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        File direct = new File(folder, trimmed);
        if (direct.isFile()) {
            return direct;
        }
        String[] extensions = {".schem", ".litematic", ".schematic"};
        for (String extension : extensions) {
            if (trimmed.toLowerCase().endsWith(extension)) {
                continue;
            }
            File candidate = new File(folder, trimmed + extension);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return direct;
    }

    public CheckResult check(String name) {
        File file = resolveFile(name);
        if (!file.isFile()) {
            return CheckResult.fail(name, "файл не найден");
        }
        if (!worldEditPresent()) {
            return CheckResult.fail(file.getName(), "WorldEdit / FAWE не установлен");
        }
        try {
            LoadedClipboard loaded = load(file);
            BlockVector3 min = loaded.clipboard.getMinimumPoint();
            BlockVector3 max = loaded.clipboard.getMaximumPoint();
            String size = (max.x() - min.x() + 1) + "x" + (max.y() - min.y() + 1) + "x" + (max.z() - min.z() + 1);
            return new CheckResult(true, file.getName(), loaded.formatName, size, "");
        } catch (Exception exception) {
            return CheckResult.fail(file.getName(), Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName()));
        }
    }

    public void pasteStartup(World world) throws Exception {
        paste(plugin.settings().startupSchematic(), world, plugin.settings().pasteX(), plugin.settings().pasteY(), plugin.settings().pasteZ());
    }

    public void paste(String name, World world, int x, int y, int z) throws Exception {
        if (!worldEditPresent()) {
            throw new IllegalStateException("WorldEdit / FAWE не установлен");
        }
        File file = resolveFile(name);
        if (!file.isFile()) {
            throw new IllegalStateException("схематика не найдена: " + name);
        }
        LoadedClipboard loaded = load(file);
        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            Operation operation = new ClipboardHolder(loaded.clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
        }
    }

    private LoadedClipboard load(File file) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new IllegalStateException("неизвестный формат файла");
        }
        try (FileInputStream stream = new FileInputStream(file);
             ClipboardReader reader = format.getReader(stream)) {
            return new LoadedClipboard(reader.read(), format.getName());
        }
    }

    private record LoadedClipboard(Clipboard clipboard, String formatName) {
    }
}
