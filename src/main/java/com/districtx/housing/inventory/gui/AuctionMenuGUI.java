package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class AuctionMenuGUI extends InventoryGUI {
    private final HousingPlugin plugin;

    public AuctionMenuGUI(HousingPlugin plugin) {
        this.plugin = plugin;
        addAction("list", event -> plugin.openAuctions((Player) event.getWhoClicked()));
        addAction("create", event -> plugin.openOwnedPremiumHouses((Player) event.getWhoClicked()));
        addAction("my-listings", event -> plugin.openMyListings((Player) event.getWhoClicked()));
    }

    private void addAction(String key, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("auction-menu." + key);
        if (section == null) return;
        for (int slot : section.getIntegerList("slots")) {
            addButton(slot, new InventoryButton()
                    .creator(player -> GuiItems.create(section, new HashMap<>()))
                    .consumer(action));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-menu.size", 54),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("auction-menu.title", "&8Auctions / Bids")));
    }
}