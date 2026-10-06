package dev.decmopl.fakekb;

import dev.decmopl.fakekb.command.FakeKBCommand;
import dev.decmopl.fakekb.config.PluginSettings;
import dev.decmopl.fakekb.game.GameListener;
import dev.decmopl.fakekb.game.GameManager;
import dev.decmopl.fakekb.game.KitService;
import dev.decmopl.fakekb.lobby.KitMenu;
import dev.decmopl.fakekb.lobby.LobbyManager;
import dev.decmopl.fakekb.util.Messages;
import dev.decmopl.fakekb.world.CageService;
import dev.decmopl.fakekb.world.SchematicService;
import dev.decmopl.fakekb.world.WorldService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class FakeKBPlugin extends JavaPlugin {

    private PluginSettings settings;
    private Messages messages;
    private SchematicService schematicService;
    private WorldService worldService;
    private CageService cageService;
    private KitService kitService;
    private LobbyManager lobbyManager;
    private GameManager gameManager;
    private KitMenu kitMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File schemDir = getSchemFolder();
        if (!schemDir.exists() && !schemDir.mkdirs()) {
            getLogger().severe("Не удалось создать папку schem: " + schemDir.getAbsolutePath());
        }

        this.settings = new PluginSettings(this);
        this.messages = new Messages(this);
        this.schematicService = new SchematicService(this);
        this.worldService = new WorldService(this);
        this.cageService = new CageService();
        this.kitService = new KitService();
        this.kitMenu = new KitMenu(this);
        this.gameManager = new GameManager(this);
        this.lobbyManager = new LobbyManager(this);

        FakeKBCommand command = new FakeKBCommand(this);
        var pluginCommand = getCommand("fakekb");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new GameListener(this), this);
        pluginManager.registerEvents(kitMenu, this);

        lobbyManager.start();
        getLogger().info("FakeKB включён. Схематики: " + schemDir.getAbsolutePath());
    }

    @Override
    public void onDisable() {
        if (lobbyManager != null) {
            lobbyManager.shutdown();
        }
        if (gameManager != null && gameManager.isRunning()) {
            gameManager.stop(false);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        settings.reload();
        messages.reload();
        lobbyManager.refreshBossBar();
    }

    public File getSchemFolder() {
        return new File(getDataFolder(), "schem");
    }

    public PluginSettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public SchematicService schematicService() {
        return schematicService;
    }

    public WorldService worldService() {
        return worldService;
    }

    public CageService cageService() {
        return cageService;
    }

    public KitService kitService() {
        return kitService;
    }

    public LobbyManager lobbyManager() {
        return lobbyManager;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public KitMenu kitMenu() {
        return kitMenu;
    }
}
