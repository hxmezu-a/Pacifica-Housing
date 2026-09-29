package com.districtx.housing.inventory.gui;

import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionsGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final List<HouseAuction> auctions = new ArrayList<>();

    public AuctionsGUI(HousingPlugin plugin) {
        this.plugin = plugin;
        for (HouseAuction auction : plugin.getAuctionManager().all()) {
            if (auctions.size() >= 54 || auction.getEndAt() <= System.currentTimeMillis()) continue;
            House house = plugin.getHouseManager().get(auction.getHouseName());
            if (house == null || house.getType() != com.districtx.housing.model.HouseType.PREMIUM) continue;
            auctions.add(auction);
        }
        auctions.sort(Comparator.comparingLong(HouseAuction::getEndAt));
        java.util.List<Integer> contentSlots = getContentSlots();
        for (int index = 0; index < auctions.size() && index < contentSlots.size(); index++) {
            HouseAuction auction = auctions.get(index);
            int slot = contentSlots.get(index);
            addButton(slot, new InventoryButton().creator(player -> createItem(auction))
                    .consumer(event -> plugin.openBid((Player) event.getWhoClicked(), auction)));
        }
    }

    private org.bukkit.inventory.ItemStack createItem(HouseAuction auction) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("auctions.item");
        return GuiItems.create(section, values(auction));
    }

    private Map<String, String> values(HouseAuction auction) {
        Map<String, String> values = new HashMap<>();
        House house = plugin.getHouseManager().get(auction.getHouseName());
        values.put("house", auction.getHouseName());
        values.put("type", house == null ? "premium" : house.getType().name().toLowerCase());
        String seller = Bukkit.getOfflinePlayer(auction.getSeller()).getName();
        values.put("seller", seller == null ? "Unknown" : seller);
        values.put("starting-diamonds", plugin.formatAmount(auction.getStartingDiamonds()));
        values.put("starting-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getStartingBalance()) : "Disabled");
        values.put("current-diamonds", plugin.formatAmount(auction.getCurrentDiamonds()));
        values.put("current-balance", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : "Disabled");
        values.put("minimum-bid", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getStartingBalance()) : plugin.formatAmount(auction.getStartingDiamonds()));
        values.put("top-bid", auction.isBalanceAllowed()
                ? plugin.formatAmount(auction.getCurrentBalance()) : (auction.getHighestBidder() == null
                ? "NONE" : plugin.formatAmount(auction.getCurrentDiamonds())));
        values.put("buyout-price", auction.isBuyoutEnabled()
                ? plugin.formatBalance(auction.getBuyoutPrice()) : "NONE");
        String bidder = auction.getHighestBidder() == null ? null : Bukkit.getOfflinePlayer(auction.getHighestBidder()).getName();
        values.put("bidder", bidder == null ? "None" : bidder);
        values.put("remaining", remaining(auction));
        values.put("status", "ACTIVE");
        return values;
    }

    private String remaining(HouseAuction auction) {
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s";
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, plugin.getGuiConfig().getInt("auctions.size", 54),
                ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("auctions.title", "&6&lAuction House")));
    }
}