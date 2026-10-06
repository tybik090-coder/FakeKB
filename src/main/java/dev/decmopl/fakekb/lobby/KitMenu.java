package dev.decmopl.fakekb.lobby;

import dev.decmopl.fakekb.FakeKBPlugin;
import dev.decmopl.fakekb.game.KitType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class KitMenu implements Listener {

    public static final Component TITLE = Component.text("Выбор кита", NamedTextColor.GOLD);
    private final FakeKBPlugin plugin;
    private final NamespacedKey selectorKey;

    public KitMenu(FakeKBPlugin plugin) {
        this.plugin = plugin;
        this.selectorKey = new NamespacedKey(plugin, "kit_selector");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 9, TITLE);
        KitType selected = plugin.kitService().get(player);
        inventory.setItem(1, KitType.ASSASSIN.menuIcon(selected == KitType.ASSASSIN));
        inventory.setItem(3, KitType.MINER.menuIcon(selected == KitType.MINER));
        inventory.setItem(5, KitType.TRAPPER.menuIcon(selected == KitType.TRAPPER));
        inventory.setItem(7, KitType.FARMER.menuIcon(selected == KitType.FARMER));
        player.openInventory(inventory);
    }

    public void giveSelector(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Выбор кита", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        meta.lore(java.util.List.of(Component.text("ПКМ — открыть меню", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        player.getInventory().setItem(4, item);
    }

    public boolean isSelector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(selectorKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        KitType type = switch (current.getType()) {
            case IRON_SWORD -> KitType.ASSASSIN;
            case DIAMOND_PICKAXE -> KitType.MINER;
            case COBWEB -> KitType.TRAPPER;
            case GOLDEN_CARROT -> KitType.FARMER;
            default -> null;
        };
        if (type == null) {
            return;
        }
        plugin.kitService().select(player, type);
        plugin.messages().send(player, "messages.kit-selected", Map.of("kit", type.displayName()));
        player.closeInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isSelector(event.getItem())) {
            return;
        }
        if (plugin.gameManager().isParticipant(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer());
    }

    public static final class Holder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }
}
