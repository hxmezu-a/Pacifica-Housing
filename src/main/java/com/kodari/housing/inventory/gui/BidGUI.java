package com.kodari.housing.inventory.gui;

import com.kodari.housing.HousingPlugin;
import com.kodari.housing.inventory.InventoryButton;
import com.kodari.housing.inventory.InventoryGUI;
import com.kodari.housing.model.HouseAuction;
import com.kodari.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class BidGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final HouseAuction auction;

    public BidGUI(HousingPlugin plugin, HouseAuction auction) {
        this.plugin = plugin;
        this.auction = auction;
        ConfigurationSection bid = plugin.getGuiConfig().getConfigurationSection("bid.place");
        for (int slot : plugin.getGuiConfig().getIntegerList("bid.place.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(bid, values()))
                    .consumer(event -> plugin.startBidInput((Player) event.getWhoClicked(), auction)));
        }
        ConfigurationSection close = plugin.getGuiConfig().getConfigurationSection("bid.close");
        if (close != null) for (int slot : close.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(close, values()))
                    .consumer(event -> event.getWhoClicked().closeInventory()));
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("bid.size", 27),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("bid.title", "&8Bid: &f{house}").replace("{house}", auction.getHouseName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", auction.getHouseName());
        values.put("current-diamonds", plugin.formatAmount(auction.getCurrentDiamonds()));
        values.put("current-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : "Disabled");
        values.put("minimum-diamonds", plugin.formatAmount(auction.getCurrentDiamonds()));
        values.put("minimum-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : "Disabled");
        values.put("highest-bidder", auction.getHighestBidder() == null
                ? "None" : Bukkit.getOfflinePlayer(auction.getHighestBidder()).getName());
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        values.put("remaining", seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s");
        values.put("status", "ACTIVE");
        values.put("bid-format", auction.isBalanceAllowed() ? "C:diamonds/balance" : "D:diamonds");
        return values;
    }
}