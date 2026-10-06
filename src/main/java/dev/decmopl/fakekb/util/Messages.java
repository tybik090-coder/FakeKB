package dev.decmopl.fakekb.util;

import dev.decmopl.fakekb.FakeKBPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final FakeKBPlugin plugin;

    public Messages(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // values are always read from current config
    }

    public Component parse(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MINI.deserialize(miniMessage);
    }

    public Component prefixed(String path, Map<String, String> placeholders) {
        return parse(apply(prefix() + raw(path), placeholders));
    }

    public Component rawComponent(String path, Map<String, String> placeholders) {
        return parse(apply(raw(path), placeholders));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefixed(path, placeholders));
    }

    public void sendHelp(CommandSender sender) {
        for (String line : helpLines()) {
            sender.sendMessage(parse(apply(line, Map.of(
                    "min", String.valueOf(plugin.settings().minPlayers()),
                    "radius", String.valueOf(plugin.settings().lobbyRadius())
            ))));
        }
    }

    public void title(Player player, String titlePath, String subPath, Map<String, String> placeholders) {
        Title title = Title.title(
                parse(apply(raw("messages.titles." + titlePath), placeholders)),
                parse(apply(raw("messages.titles." + subPath), placeholders)),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200))
        );
        player.showTitle(title);
    }

    public BossBar.Color barColor(String name, BossBar.Color fallback) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public String prefix() {
        return plugin.getConfig().getString("messages.prefix", "");
    }

    public List<String> helpLines() {
        return plugin.getConfig().getStringList("messages.help");
    }

    public String raw(String path) {
        return plugin.getConfig().getString(path, "<red>missing: " + path + "</red>");
    }

    public static String apply(String text, Map<String, String> placeholders) {
        String result = text;
        for (var entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
