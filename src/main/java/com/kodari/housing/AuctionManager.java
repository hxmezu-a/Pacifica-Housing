package com.kodari.housing;

import com.kodari.housing.model.HouseAuction;
import com.kodari.housing.model.CommittedBid;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionManager {
    private final HousingPlugin plugin;
    private final File file;
    private final Map<String, HouseAuction> auctions = new HashMap<>();
    private FileConfiguration data;

    public AuctionManager(HousingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        load();
    }

    public void load() {
        data = YamlConfiguration.loadConfiguration(file);
        auctions.clear();
        ConfigurationSection root = data.getConfigurationSection("auctions");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            try {
                double startingDiamonds = section.getDouble("starting-diamonds");
                double startingBalance = section.getDouble("starting-balance");
                if (startingDiamonds <= 0) continue;
                boolean balanceAllowed = section.contains("balance-allowed")
                        ? section.getBoolean("balance-allowed") : startingBalance > 0;
                HouseAuction auction = new HouseAuction(
                        section.getString("house", key),
                        UUID.fromString(section.getString("seller")),
                        startingDiamonds,
                        startingBalance,
                        balanceAllowed,
                        section.getLong("end-at"));
                auction.setCurrentDiamonds(section.getDouble("current-diamonds", auction.getStartingDiamonds()));
                auction.setCurrentBalance(section.getDouble("current-balance", auction.getStartingBalance()));
                String bidder = section.getString("highest-bidder");
                if (bidder != null && !bidder.isEmpty()) auction.setHighestBidder(UUID.fromString(bidder));
                auction.setSettlementState(section.getString("settlement-state", "ACTIVE"));
                auction.setSellerDiamondsPaid(section.getBoolean("seller-diamonds-paid", false));
                auction.setSellerBalancePaid(section.getBoolean("seller-balance-paid", false));
                auction.setOwnershipTransferred(section.getBoolean("ownership-transferred", false));
                ConfigurationSection bids = section.getConfigurationSection("committed-bids");
                if (bids != null) {
                    for (String bidId : bids.getKeys(false)) {
                        ConfigurationSection bid = bids.getConfigurationSection(bidId);
                        if (bid == null) continue;
                        String bidderId = bid.getString("bidder");
                        if (bidderId == null) continue;
                        CommittedBid committedBid = new CommittedBid(
                                bidId,
                                UUID.fromString(bidderId),
                                bid.getDouble("diamonds"),
                                bid.getDouble("balance"),
                                bid.getBoolean("diamonds-held", false),
                                bid.getBoolean("balance-held", false),
                                bid.getString("status", "HELD"));
                        auction.getCommittedBids().put(bidId, committedBid);
                    }
                }
                if (auction.getCommittedBids().isEmpty() && auction.getHighestBidder() != null) {
                    String legacyId = "legacy-" + auction.getHighestBidder();
                    auction.getCommittedBids().put(legacyId, new CommittedBid(
                            legacyId,
                            auction.getHighestBidder(),
                            auction.getCurrentDiamonds(),
                            auction.getCurrentBalance(),
                            true,
                            auction.isBalanceAllowed() && auction.getCurrentBalance() > 0,
                            "HELD"));
                }
                if (plugin.getHouseManager().get(auction.getHouseName()) != null) {
                    auctions.put(auction.getHouseName().toLowerCase(), auction);
                }
            } catch (IllegalArgumentException | NullPointerException ignored) {
            }
        }
    }

    public void save() {
        data.set("auctions", null);
        for (HouseAuction auction : auctions.values()) {
            String path = "auctions." + auction.getHouseName();
            data.set(path + ".house", auction.getHouseName());
            data.set(path + ".seller", auction.getSeller().toString());
            data.set(path + ".starting-diamonds", auction.getStartingDiamonds());
            data.set(path + ".starting-balance", auction.getStartingBalance());
            data.set(path + ".balance-allowed", auction.isBalanceAllowed());
            data.set(path + ".current-diamonds", auction.getCurrentDiamonds());
            data.set(path + ".current-balance", auction.getCurrentBalance());
            data.set(path + ".highest-bidder", auction.getHighestBidder() == null ? null : auction.getHighestBidder().toString());
            data.set(path + ".end-at", auction.getEndAt());
            data.set(path + ".settlement-state", auction.getSettlementState());
            data.set(path + ".seller-diamonds-paid", auction.isSellerDiamondsPaid());
            data.set(path + ".seller-balance-paid", auction.isSellerBalancePaid());
            data.set(path + ".ownership-transferred", auction.isOwnershipTransferred());
            for (CommittedBid bid : auction.getCommittedBids().values()) {
                String bidPath = path + ".committed-bids." + bid.getId();
                data.set(bidPath + ".bidder", bid.getBidder().toString());
                data.set(bidPath + ".diamonds", bid.getDiamonds());
                data.set(bidPath + ".balance", bid.getBalance());
                data.set(bidPath + ".diamonds-held", bid.isDiamondsHeld());
                data.set(bidPath + ".balance-held", bid.isBalanceHeld());
                data.set(bidPath + ".status", bid.getStatus());
            }
        }
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save auctions.yml: " + exception.getMessage());
        }
    }

    public HouseAuction get(String houseName) {
        return houseName == null ? null : auctions.get(houseName.toLowerCase());
    }

    public Collection<HouseAuction> all() {
        return new ArrayList<>(auctions.values());
    }

    public boolean create(HouseAuction auction) {
        if (get(auction.getHouseName()) != null) return false;
        auctions.put(auction.getHouseName().toLowerCase(), auction);
        save();
        return true;
    }

    public void remove(HouseAuction auction) {
        if (auction != null && auctions.remove(auction.getHouseName().toLowerCase()) != null) save();
    }
}