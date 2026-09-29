package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.model.House;
import com.districtx.housing.util.GuiItems;
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
        ConfigurationSection house = plugin.getGuiConfig().getConfigurationSection("bid.house");
        if (house != null) for (int slot : house.getIntegerList("slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(house, values()))
                    .consumer(event -> { }));
        }
        ConfigurationSection bid = plugin.getGuiConfig().getConfigurationSection("bid.place");
        for (int slot : plugin.getGuiConfig().getIntegerList("bid.place.slots")) {
            addButton(slot, new InventoryButton().creator(player -> GuiItems.create(bid, values()))
                    .consumer(event -> plugin.openBidInput((Player) event.getWhoClicked(), auction)));
        }
        ConfigurationSection buyout = plugin.getGuiConfig().getConfigurationSection("bid.buyout");
        if (auction.isBuyoutEnabled() && buyout != null) {
            for (int slot : buyout.getIntegerList("slots")) {
                addButton(slot, new InventoryButton().creator(player -> GuiItems.create(buyout, values()))
                        .consumer(event -> plugin.openBuyoutConfirmation((Player) event.getWhoClicked(), auction)));
            }
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("bid.size", 54),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("bid.title", "&8Bid: &f{house}").replace("{house}", auction.getHouseName())));
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("house", auction.getHouseName());
        House house = plugin.getHouseManager().get(auction.getHouseName());
        values.put("type", house == null ? "premium" : house.getType().name().toLowerCase());
        values.put("price", house == null ? "Unknown" : plugin.formatAmount(house.getPrice()));
        String seller = Bukkit.getOfflinePlayer(auction.getSeller()).getName();
        values.put("seller", seller == null ? "Unknown" : seller);
        values.put("starting-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getStartingBalance()) : "Disabled");
        values.put("current-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : "Disabled");
        values.put("minimum-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : "Disabled");
        values.put("starting-diamonds", plugin.formatAmount(auction.getStartingDiamonds()));
        values.put("minimum-diamonds", plugin.formatAmount(auction.getStartingDiamonds()));
        values.put("current-diamonds", plugin.formatAmount(auction.getCurrentDiamonds()));
        values.put("buyout-price", auction.isBuyoutEnabled()
                ? plugin.formatBalance(auction.getBuyoutPrice()) : "NONE");
        values.put("diamonds", plugin.formatAmount(auction.getStartingDiamonds()));
        values.put("highest-bidder", auction.getHighestBidder() == null
                ? "None" : Bukkit.getOfflinePlayer(auction.getHighestBidder()).getName());
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        values.put("remaining", seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s");
        values.put("status", "ACTIVE");
        values.put("bid-format", "Diamond amount only");
        return values;
    }
}