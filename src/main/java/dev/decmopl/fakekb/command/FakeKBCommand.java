package dev.decmopl.fakekb.command;

import dev.decmopl.fakekb.FakeKBPlugin;
import dev.decmopl.fakekb.world.SchematicService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FakeKBCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of(
            "help", "setlobby", "reload", "start", "stop", "rangelobby", "check"
    );

    private final FakeKBPlugin plugin;

    public FakeKBCommand(FakeKBPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!sender.hasPermission("fakekb.use") && !sender.hasPermission("fakekb.admin")) {
                plugin.messages().send(sender, "messages.no-permission");
                return true;
            }
            plugin.messages().sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sub.equals("help") && !sender.hasPermission("fakekb.admin")) {
            plugin.messages().send(sender, "messages.no-permission");
            return true;
        }

        switch (sub) {
            case "setlobby" -> setLobby(sender);
            case "reload" -> {
                plugin.reloadPlugin();
                plugin.messages().send(sender, "messages.reload");
            }
            case "start" -> start(sender);
            case "stop" -> stop(sender);
            case "rangelobby" -> rangeLobby(sender, args);
            case "check" -> check(sender, args);
            default -> plugin.messages().sendHelp(sender);
        }
        return true;
    }

    private void setLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игрок может установить лобби.");
            return;
        }
        plugin.settings().saveLobby(player.getLocation());
        plugin.lobbyManager().refreshBossBar();
        plugin.messages().send(player, "messages.lobby-set", Map.of(
                "radius", String.valueOf(plugin.settings().lobbyRadius())
        ));
        plugin.kitMenu().giveSelector(player);
    }

    private void start(CommandSender sender) {
        if (plugin.gameManager().isRunning()) {
            plugin.messages().send(sender, "messages.already-running");
            return;
        }
        if (!plugin.settings().lobbySet() || plugin.settings().lobbyLocation() == null) {
            plugin.messages().send(sender, "messages.lobby-not-set");
            return;
        }
        var players = plugin.lobbyManager().playersInZone();
        if (players.isEmpty()) {
            plugin.messages().send(sender, "messages.start-no-players");
            return;
        }
        plugin.messages().send(sender, "messages.start-forced");
        plugin.gameManager().start(players, true);
    }

    private void stop(CommandSender sender) {
        if (!plugin.gameManager().isRunning()) {
            plugin.messages().send(sender, "messages.not-running");
            return;
        }
        plugin.gameManager().stop(true);
    }

    private void rangeLobby(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "messages.radius-usage");
            plugin.messages().send(sender, "messages.radius-set", Map.of(
                    "radius", String.valueOf(plugin.settings().lobbyRadius())
            ));
            return;
        }
        try {
            int radius = Integer.parseInt(args[1]);
            plugin.settings().lobbyRadius(radius);
            plugin.messages().send(sender, "messages.radius-set", Map.of("radius", String.valueOf(plugin.settings().lobbyRadius())));
        } catch (NumberFormatException exception) {
            plugin.messages().send(sender, "messages.radius-usage");
        }
    }

    private void check(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "messages.check-usage");
            return;
        }
        if (!plugin.schematicService().worldEditPresent()) {
            plugin.messages().send(sender, "messages.worldedit-missing");
        }
        SchematicService.CheckResult result = plugin.schematicService().check(args[1]);
        if (result.ok()) {
            plugin.messages().send(sender, "messages.schematic-ok", Map.of(
                    "name", result.name(),
                    "format", result.format(),
                    "size", result.size()
            ));
        } else if (result.error().contains("не найден")) {
            plugin.messages().send(sender, "messages.schematic-missing", Map.of("name", args[1]));
        } else {
            plugin.messages().send(sender, "messages.schematic-fail", Map.of(
                    "name", result.name(),
                    "error", result.error()
            ));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            File folder = plugin.getSchemFolder();
            File[] files = folder.listFiles();
            if (files == null) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    names.add(file.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
