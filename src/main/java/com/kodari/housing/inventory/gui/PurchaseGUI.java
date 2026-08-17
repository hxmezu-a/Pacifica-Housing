package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.House;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class PurchaseGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public PurchaseGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        Map<String, String> values = values();
        ConfigurationSection buy = plugin.getGuiConfig().getConfigurationSection("purchase.buy");
        for (int slot : plugin.getGuiConfig().getIntegerList("purchase.buy.slots")) {
            addButton(slot, new InventoryButton()
                    .creator(player -> GuiItems.create(buy, values))
                    .consumer(event -> plugin.purchaseHouse((Player) event.getWhoClicked(), house)));
        }
        ConfigurationSection cancel = plugin.getGuiConfig().getConfigurationSection("purchase.cancel");
        for (int slot : plugin.getGuiConfig().getIntegerList("purchase.cancel.slots")) {
            addButton(slot, new InventoryButton()
                    .creator(player -> GuiItems.create(cancel, values))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("purchase.size", 9),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("purchase.title", "Purchase {house}")
                                .replace("{house}", house.getName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("type", house.getType().name().toLowerCase());
        values.put("price", plugin.formatAmount(house.getPrice()));
        return values;
    }
}