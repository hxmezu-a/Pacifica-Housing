package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseType;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AvailableHouseDetailsGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;
    private final Map<String, String> values;

    public AvailableHouseDetailsGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        this.values = values();
        addButton(11, new InventoryButton().creator(player -> createCostItem()));
        addButton(13, new InventoryButton().creator(player -> createItem("BOOK", "&6&lHouse Info",
                "&7House ID: &6" + house.getName(),
                "&7House Type: &6" + values.get("type"))));
        addButton(15, new InventoryButton().creator(player -> createItem("BARREL",
                "&7House Vaults: &6" + house.getVaults().size()))
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (house.getType() == HouseType.REGULAR) {
                        plugin.getMessages().send(player, "vault-unavailable", Collections.emptyMap());
                        return;
                    }
                    plugin.openVaults(player, house);
                }));
        addButton(31, new InventoryButton().creator(player -> createItem("SLIME_BALL", "&a&lBuy House",
                "&7Click to buy"))
                .consumer(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (house.getOwner() != null) {
                        plugin.getMessages().send(player, "already-owned", Collections.emptyMap());
                        player.closeInventory();
                        return;
                    }
                    plugin.openPurchase(player, house);
                }));
        addButton(49, new InventoryButton().creator(player -> createItem(typeMaterial(),
                "&9&lHouse " + house.getName())));
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("available-details.title", "&8House: &f{house}")
                                .replace("{house}", house.getName())));
    }

    @Override
    public void decorate(Player player) {
        getInventory().clear();
        super.decorate(player);
    }

    private ItemStack createCostItem() {
        if (house.getType() == HouseType.REGULAR) {
            return createItem("PAPER", "&7Cost: &a$" + plugin.formatAmount(house.getPrice()));
        }
        if (house.getType() == HouseType.PREMIUM) {
            return createItem("PAPER", "&7Cost: &b&l" + plugin.formatAmount(house.getPrice()) + " diamonds");
        }
        return createItem("PAPER", "&7Cost: &6&l$" + plugin.formatAmount(house.getPrice()));
    }

    private ItemStack createItem(String material, String name, String... lore) {
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.stream(lore)
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(java.util.stream.Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private String typeMaterial() {
        if (house.getType() == HouseType.REGULAR) {
            return "OAK_DOOR";
        }
        if (house.getType() == HouseType.PREMIUM) {
            return "IRON_DOOR";
        }
        return "CRIMSON_DOOR";
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        values.put("price", plugin.formatAmount(house.getPrice()));
        values.put("availability", house.getOwner() == null ? "Available" : "Unavailable");
        values.put("vault_counts", String.valueOf(house.getVaults().size()));
        return values;
    }
}