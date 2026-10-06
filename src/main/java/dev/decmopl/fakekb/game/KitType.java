package dev.decmopl.fakekb.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum KitType {
    ASSASSIN("Убийца", Material.IRON_SWORD, NamedTextColor.RED),
    MINER("Шахтёр", Material.DIAMOND_PICKAXE, NamedTextColor.AQUA),
    TRAPPER("Трапер", Material.COBWEB, NamedTextColor.DARK_PURPLE),
    FARMER("Фермер", Material.GOLDEN_CARROT, NamedTextColor.GREEN);

    private final String displayName;
    private final Material icon;
    private final NamedTextColor color;

    KitType(String displayName, Material icon, NamedTextColor color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public NamedTextColor color() {
        return color;
    }

    public ItemStack menuIcon(boolean selected) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, color));
        meta.lore(java.util.List.of(
                Component.text(selected ? "Выбран" : "Нажмите, чтобы выбрать",
                        selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static KitType fromName(String name) {
        if (name == null) {
            return ASSASSIN;
        }
        try {
            return KitType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            for (KitType type : values()) {
                if (type.displayName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return ASSASSIN;
        }
    }
}
