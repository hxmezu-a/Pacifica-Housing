package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.HouseType;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class RealEstateAgentGUI extends InventoryGUI {
    private final HousingPlugin plugin;

    public RealEstateAgentGUI(HousingPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection available = plugin.getGuiConfig().getConfigurationSection("real-estate-agent.available");
        if (available != null) for (int slot : available.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(available, new HashMap<>()))
                    .consumer(event -> plugin.openAvailableCategories((Player) event.getWhoClicked())));
        }
        ConfigurationSection auctions = plugin.getGuiConfig().getConfigurationSection("real-estate-agent.auctions");
        if (auctions != null) for (int slot : auctions.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(auctions, new HashMap<>()))
                    .consumer(event -> plugin.openAuctionMenu((Player) event.getWhoClicked())));
        }
    }

    private void addCategory(String key, HouseType type) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("real-estate-agent." + key);
        if (section == null) return;
        for (int slot : section.getIntegerList("slots")) {
            addButton(slot, new InventoryButton()
                    .creator(player -> GuiItems.create(section, new HashMap<>()))
                    .consumer(event -> plugin.openAvailableHouses((Player) event.getWhoClicked(), type)));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("real-estate-agent.size", 27),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("real-estate-agent.title", "&8Real Estate Agent")));
    }
}