package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.util.GuiItems;
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
        addAction("information", event -> plugin.openAuctionInformation((Player) event.getWhoClicked()));
        addClose("close");
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

    private void addClose(String key) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("auction-menu." + key);
        if (section == null) return;
        for (int slot : section.getIntegerList("slots")) {
            addButton(slot, new InventoryButton()
                    .creator(player -> GuiItems.create(section, new HashMap<>()))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-menu.size", 27),
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString("auction-menu.title", "&8Auctions / Bids")));
    }
}