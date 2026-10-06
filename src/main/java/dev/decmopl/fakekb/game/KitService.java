package dev.decmopl.fakekb.game;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionType;

import java.util.Map;
import java.util.UUID;

public final class KitService {

    private final Map<UUID, KitType> kits = new java.util.concurrent.ConcurrentHashMap<>();

    public KitType get(Player player) {
        return kits.getOrDefault(player.getUniqueId(), KitType.ASSASSIN);
    }

    public void select(Player player, KitType type) {
        kits.put(player.getUniqueId(), type);
    }

    public void clear(Player player) {
        kits.remove(player.getUniqueId());
    }

    public void give(Player player) {
        give(player, get(player));
    }

    public void give(Player player, KitType type) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        switch (type) {
            case ASSASSIN -> giveAssassin(inventory);
            case MINER -> giveMiner(inventory);
            case TRAPPER -> giveTrapper(inventory);
            case FARMER -> giveFarmer(inventory);
        }
        player.updateInventory();
    }

    private void giveAssassin(PlayerInventory inventory) {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
        inventory.addItem(sword);
        inventory.addItem(new ItemStack(Material.ENDER_PEARL, 8));
        inventory.addItem(potion(PotionType.INVISIBILITY, 2));
        inventory.addItem(potion(PotionType.SWIFTNESS, 2));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 16));
        inventory.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inventory.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }

    private void giveMiner(PlayerInventory inventory) {
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        pick.addUnsafeEnchantment(Enchantment.EFFICIENCY, 4);
        pick.addUnsafeEnchantment(Enchantment.UNBREAKING, 2);
        inventory.addItem(pick);
        inventory.addItem(new ItemStack(Material.IRON_SHOVEL));
        inventory.addItem(new ItemStack(Material.IRON_AXE));
        inventory.addItem(new ItemStack(Material.TORCH, 64));
        inventory.addItem(new ItemStack(Material.COBBLESTONE, 64));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 32));
        inventory.setHelmet(new ItemStack(Material.IRON_HELMET));
        inventory.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void giveTrapper(PlayerInventory inventory) {
        inventory.addItem(new ItemStack(Material.STONE_SWORD));
        inventory.addItem(new ItemStack(Material.COBWEB, 32));
        inventory.addItem(new ItemStack(Material.OBSIDIAN, 16));
        inventory.addItem(new ItemStack(Material.TNT, 8));
        inventory.addItem(new ItemStack(Material.FLINT_AND_STEEL));
        inventory.addItem(new ItemStack(Material.LAVA_BUCKET, 2));
        inventory.addItem(new ItemStack(Material.WATER_BUCKET));
        inventory.addItem(new ItemStack(Material.OAK_PRESSURE_PLATE, 16));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 16));
        inventory.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inventory.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }

    private void giveFarmer(PlayerInventory inventory) {
        inventory.addItem(new ItemStack(Material.IRON_HOE));
        inventory.addItem(new ItemStack(Material.GOLDEN_CARROT, 32));
        inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4));
        inventory.addItem(new ItemStack(Material.BONE_MEAL, 32));
        inventory.addItem(new ItemStack(Material.WATER_BUCKET, 2));
        inventory.addItem(new ItemStack(Material.HAY_BLOCK, 16));
        inventory.addItem(new ItemStack(Material.BREAD, 32));
        inventory.addItem(new ItemStack(Material.SHIELD));
        inventory.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inventory.setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }

    private ItemStack potion(PotionType type, int amount) {
        ItemStack item = new ItemStack(Material.POTION, amount);
        item.editMeta(org.bukkit.inventory.meta.PotionMeta.class, meta -> meta.setBasePotionType(type));
        return item;
    }
}
