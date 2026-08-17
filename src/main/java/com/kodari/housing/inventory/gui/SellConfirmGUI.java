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

public class SellConfirmGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public SellConfirmGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        Map<String, String> values = values();
        ConfigurationSection confirm = plugin.getGuiConfig().getConfigurationSection("sell-confirm.confirm");
        for (int slot : plugin.getGuiConfig().getIntegerList("sell-confirm.confirm.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(confirm, values))
                    .consumer(event -> plugin.sellHouse((Player) event.getWhoClicked(), house)));
        }
        ConfigurationSection cancel = plugin.getGuiConfig().getConfigurationSection("sell-confirm.cancel");
        for (int slot : plugin.getGuiConfig().getIntegerList("sell-confirm.cancel.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(cancel, values))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("sell-confirm.size", 9),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig()
                        .getString("sell-confirm.title", "Sell House: {house}")
                        .replace("{house}", house.getName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        values.put("refund", plugin.formatAmount(plugin.refundAmount(house)));
        return values;
    }
}