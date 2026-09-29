package com.districtx.housing.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import com.districtx.housing.HousingPlugin;
import com.districtx.housing.inventory.InventoryButton;
import com.districtx.housing.inventory.InventoryGUI;
import com.districtx.housing.model.HouseAuction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AuctionManagementGUI extends InventoryGUI {
    private final HousingPlugin plugin;
    private final UUID ownerUuid;
    private final String auctionId;

    public AuctionManagementGUI(HousingPlugin plugin, UUID ownerUuid, String auctionId) {
        this.plugin = plugin;
        this.ownerUuid = ownerUuid;
        this.auctionId = auctionId;
    }

    @Override
    public void decorate(Player player) {
        for (int slot : getContentSlots()) {
            removeButton(slot);
        }

        HouseAuction auction = plugin.getAuctionManager().getActiveListing(auctionId, ownerUuid);
        if (auction == null) {
            addButton(22, new InventoryButton().creator(viewer -> createItem("BARRIER", "&cListing unavailable",
                    Arrays.asList("&7This auction is no longer active."))));
        } else {
            addButton(22, new InventoryButton().creator(viewer -> createAuctionDetails(auction)));
            if (plugin.getAuctionManager().canCancelAuction(auctionId, ownerUuid)) {
                addButton(31, new InventoryButton()
                        .creator(viewer -> createItem("RED_WOOL", "&c&lCancel Auction",
                                Arrays.asList("&7This listing has no bids.", "&eClick to cancel")))
                        .consumer(event -> plugin.cancelAuction((Player) event.getWhoClicked(), auctionId)));
            } else {
                addButton(31, new InventoryButton().creator(viewer -> createItem("BARRIER", "&cCannot cancel",
                        Arrays.asList("&7This auction has active bids."))));
            }
        }
        super.decorate(player);
    }

    private ItemStack createAuctionDetails(HouseAuction auction) {
        String currentBid = auction.getHighestBidder() == null ? "None"
                : auction.isBalanceAllowed() ? plugin.formatBalance(auction.getCurrentBalance())
                : plugin.formatAmount(auction.getCurrentDiamonds());
        String bidder = auction.getHighestBidder() == null ? "None"
                : Bukkit.getOfflinePlayer(auction.getHighestBidder()).getName();
        if (bidder == null) {
            bidder = "Unknown";
        }
        String minimumBid = auction.isBalanceAllowed() ? plugin.formatBalance(auction.getStartingBalance())
                : plugin.formatAmount(auction.getStartingDiamonds());
        String buyout = auction.isBuyoutEnabled() ? plugin.formatBalance(auction.getBuyoutPrice()) : "None";
        long seconds = Math.max(0, (auction.getEndAt() - System.currentTimeMillis()) / 1000L);
        String remaining = seconds / 3600 + "h " + (seconds % 3600) / 60 + "m " + seconds % 60 + "s";
        return createItem("IRON_DOOR", "&b&l" + auction.getHouseName(), Arrays.asList(
                "&7House: &f" + auction.getHouseName(),
                "&7Minimum Bid: &b" + minimumBid,
                "&7Current Bid: &b" + currentBid,
                "&7Highest Bidder: &f" + bidder,
                "&7Buyout: &a" + buyout,
                "&7Ends: &f" + remaining));
    }

    private ItemStack createItem(String material, String name, List<String> loreLines) {
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', "&6&lAuction Listing"));
    }
}