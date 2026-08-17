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

public class AuctionInformationGUI extends InventoryGUI {
    private final HousingPlugin plugin;

    public AuctionInformationGUI(HousingPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection info = plugin.getGuiConfig().getConfigurationSection("auction-information.info");
        if (info != null) for (int slot : info.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(info, new HashMap<>()))
                    .consumer(event -> {
                    }));
        }
        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("auction-information.close");
        if (close != null) for (int slot : close.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, new HashMap<>()))
                    .consumer(event -> ((Player) event.getWhoClicked()).closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auction-information.size", 27),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString(
                        "auction-information.title", "&8Auction Information")));
    }
}