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

public class AuctionSetupGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final House house;

    public AuctionSetupGUI(HousingPlugin plugin, House house) {
        this.plugin = plugin;
        this.house = house;
        addInput("diamonds");
        addInput("both");
        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("auction-setup.close");
        if (close != null) for (int slot : close.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, values()))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    private void addInput(String key) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("auction-setup." + key);
        if (section == null) return;
        for (int slot : section.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(section, values()))
                    .consumer(event -> plugin.startAuctionInput((Player) event.getWhoClicked(), house, key)));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-setup.size", 27),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("auction-setup.title", "&8Auction: &f{house}").replace("{house}", house.getName())));
    }

    private java.util.Map<String, String> values() {
        java.util.Map<String, String> values = new HashMap<>();
        values.put("house", house.getName());
        return values;
    }
}