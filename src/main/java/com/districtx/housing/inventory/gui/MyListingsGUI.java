package com.districtx.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.districtx.housing.HousingPlugin;
import com.districtx.housing.api.AuctionInfo;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.util.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyListingsGUI extends InventoryGUI {
    private final HousingPlugin plugin;

    public MyListingsGUI(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void decorate(Player player) {
        List<Integer> contentSlots = getContentSlots();
        for (int slot : contentSlots) {
            removeButton(slot);
        }

        List<AuctionInfo> listings = new ArrayList<>(plugin.getHousingApi().auctions()
                .getActiveListings(player.getUniqueId()));
        listings.sort(Comparator.comparingLong(AuctionInfo::getCreatedAt)
                .thenComparing(AuctionInfo::getHouseName, String.CASE_INSENSITIVE_ORDER));

        Set<Integer> listingSlots = new HashSet<>();
        for (int index = 0; index < listings.size() && index < contentSlots.size(); index++) {
            AuctionInfo listing = listings.get(index);
            String auctionId = listing.getAuctionId();
            int slot = contentSlots.get(index);
            listingSlots.add(slot);
            addButton(slot, new InventoryButton()
                    .creator(viewer -> createListingItem(listing))
                    .consumer(event -> plugin.openAuctionManagement((Player) event.getWhoClicked(), auctionId)));
        }

        super.decorate(player);

        for (int slot : contentSlots) {
            if (!listingSlots.contains(slot)) {
                getInventory().setItem(slot, createPane(listings.isEmpty()
                        ? "GRAY_STAINED_GLASS_PANE" : "LIGHT_GRAY_STAINED_GLASS_PANE"));
            }
        }
        if (listings.isEmpty()) {
            getInventory().setItem(22, createNoListingsItem());
        }
    }

    private ItemStack createListingItem(AuctionInfo auction) {
        ConfigurationSection section = plugin.getGuiConfig().getConfigurationSection("auctions.item");
        return GuiItems.create(section, values(auction));
    }

    private Map<String, String> values(AuctionInfo auction) {
        Map<String, String> values = new HashMap<>();
        values.put("house", auction.getHouseName());
        values.put("type", "premium");
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
                ? auction.getHighestBidder() == null ? "NONE" : plugin.formatAmount(auction.getCurrentBalance())
                : auction.getHighestBidder() == null ? "NONE" : plugin.formatAmount(auction.getCurrentDiamonds()));
        values.put("buyout-price", auction.isBuyoutEnabled()
                ? plugin.formatBalance(auction.getBuyoutPrice()) : "NONE");
        String bidder = auction.getHighestBidder() == null ? null
                : Bukkit.getOfflinePlayer(auction.getHighestBidder()).getName();
        values.put("bidder", bidder == null ? "None" : bidder);
        values.put("remaining", remaining(auction));
        values.put("status", "ACTIVE");
        return values;
    }

    private String remaining(AuctionInfo auction) {
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s";
    }

    private ItemStack createPane(String material) {
        ItemStack pane = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (pane == null) {
            return null;
        }
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7"));
            meta.setLore(new ArrayList<>());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createNoListingsItem() {
        ItemStack barrier = XMaterial.matchXMaterial("BARRIER").map(XMaterial::parseItem).orElse(null);
        if (barrier == null) {
            return null;
        }
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cNo active listings"));
            List<String> lore = Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&7"),
                    ChatColor.translateAlternateColorCodes('&', "&7You have no premium houses up"),
                    ChatColor.translateAlternateColorCodes('&', "&7for auction right now."));
            meta.setLore(lore);
            barrier.setItemMeta(meta);
        }
        return barrier;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&',
                plugin.getGuiConfig().getString("my-listings.title", "&8My Listings")));
    }
}