package com.districtx.housing;

import com.districtx.housing.model.HouseAuction;
import com.districtx.housing.model.CommittedBid;
import com.districtx.housing.model.House;
import com.districtx.housing.model.HouseType;
import com.districtx.housing.api.auction.AuctionCancellationResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionManager {
    private final HousingPlugin plugin;
    private final File file;
    private final Map<String, HouseAuction> auctions = new HashMap<>();
    private final Map<String, HouseAuction> auctionsById = new HashMap<>();
    private FileConfiguration data;

    public AuctionManager(HousingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        load();
    }

    public void load() {
        data = YamlConfiguration.loadConfiguration(file);
        auctions.clear();
        auctionsById.clear();
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
                        section.getString("auction-id", UUID.randomUUID().toString()),
                        section.getString("house", key),
                        UUID.fromString(section.getString("seller")),
                        startingDiamonds,
                        startingBalance,
                        balanceAllowed,
                        section.getLong("end-at"),
                        section.getLong("created-at", System.currentTimeMillis()));
                auction.setCurrentDiamonds(section.contains("current-diamonds")
                        ? section.getDouble("current-diamonds") : auction.getStartingDiamonds());
                auction.setCurrentBalance(section.contains("current-balance")
                        ? section.getDouble("current-balance") : auction.getStartingBalance());
                String bidder = section.getString("highest-bidder");
                if (bidder != null && !bidder.isEmpty()) auction.setHighestBidder(UUID.fromString(bidder));
                auction.setSettlementState(section.getString("settlement-state", "ACTIVE"));
                auction.setSellerDiamondsPaid(section.getBoolean("seller-diamonds-paid", false));
                auction.setSellerBalancePaid(section.getBoolean("seller-balance-paid", false));
                auction.setOwnershipTransferred(section.getBoolean("ownership-transferred", false));
                auction.setBuyoutEnabled(section.getBoolean("buyout-enabled", false));
                auction.setBuyoutPrice(section.getDouble("buyout-price", 0));
                auction.setListingFee(section.getDouble("listing-fee", 0));
                auction.setListingFeePercentage(section.getDouble("listing-fee-percentage", 0));
                auction.setSellerRank(section.getString("seller-rank", "default"));
                auction.setListingFeeRefunded(section.getBoolean("listing-fee-refunded", false));
                auction.setListingFeePaid(section.getBoolean("listing-fee-paid", false));
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
                    if (auction.getAuctionId() != null && !auction.getAuctionId().isEmpty()) {
                        auctionsById.put(auction.getAuctionId(), auction);
                    }
                }
            } catch (IllegalArgumentException | NullPointerException ignored) {
            }
        }
    }

    public boolean save() {
        data.set("auctions", null);
        for (HouseAuction auction : auctions.values()) {
            String path = "auctions." + auction.getHouseName();
            data.set(path + ".house", auction.getHouseName());
            data.set(path + ".auction-id", auction.getAuctionId());
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
            data.set(path + ".buyout-enabled", auction.isBuyoutEnabled());
            data.set(path + ".buyout-price", auction.getBuyoutPrice());
            data.set(path + ".listing-fee", auction.getListingFee());
            data.set(path + ".listing-fee-percentage", auction.getListingFeePercentage());
            data.set(path + ".seller-rank", auction.getSellerRank());
            data.set(path + ".created-at", auction.getCreatedAt());
            data.set(path + ".listing-fee-refunded", auction.isListingFeeRefunded());
            data.set(path + ".listing-fee-paid", auction.isListingFeePaid());
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
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save auctions.yml: " + exception.getMessage());
            return false;
        }
    }

    public HouseAuction get(String houseName) {
        return houseName == null ? null : auctions.get(houseName.toLowerCase());
    }

    public HouseAuction getByAuctionId(String auctionId) {
        return auctionId == null ? null : auctionsById.get(auctionId);
    }

    public HouseAuction findAuction(String houseNameOrAuctionId) {
        HouseAuction auction = get(houseNameOrAuctionId);
        return auction != null ? auction : getByAuctionId(houseNameOrAuctionId);
    }

    public List<HouseAuction> getActiveListings(UUID ownerUuid) {
        List<HouseAuction> listings = new ArrayList<>();
        if (ownerUuid == null) {
            return listings;
        }
        for (HouseAuction auction : auctions.values()) {
            if (isActiveListing(auction, ownerUuid)) {
                listings.add(auction);
            }
        }
        return listings;
    }

    public HouseAuction getActiveListing(String houseNameOrAuctionId, UUID ownerUuid) {
        HouseAuction auction = findAuction(houseNameOrAuctionId);
        return isActiveListing(auction, ownerUuid) ? auction : null;
    }

    public boolean hasBids(String houseNameOrAuctionId) {
        HouseAuction auction = findAuction(houseNameOrAuctionId);
        return auction != null && hasBids(auction);
    }

    public boolean canCancelAuction(String houseNameOrAuctionId, UUID ownerUuid) {
        HouseAuction auction = getActiveListing(houseNameOrAuctionId, ownerUuid);
        return auction != null && !hasBids(auction);
    }

    public synchronized boolean cancelAuction(String houseNameOrAuctionId, UUID ownerUuid) {
        return cancelAuctionDetailed(houseNameOrAuctionId, ownerUuid) == AuctionCancellationResult.SUCCESS;
    }

    public synchronized AuctionCancellationResult cancelAuctionDetailed(String houseNameOrAuctionId,
                                                                         UUID ownerUuid) {
        HouseAuction auction = findAuction(houseNameOrAuctionId);
        if (auction == null) {
            return AuctionCancellationResult.AUCTION_NOT_FOUND;
        }
        if (ownerUuid == null || !ownerUuid.equals(auction.getSeller())) {
            return AuctionCancellationResult.NOT_OWNER;
        }
        String state = auction.getSettlementState();
        if ("CANCELLED".equalsIgnoreCase(state) || "CANCELED".equalsIgnoreCase(state)) {
            return AuctionCancellationResult.ALREADY_CANCELLED;
        }
        if (!"ACTIVE".equalsIgnoreCase(state) || auction.getEndAt() <= System.currentTimeMillis()
                || auction.isOwnershipTransferred()) {
            return AuctionCancellationResult.ALREADY_COMPLETED;
        }
        if (!isActiveListing(auction, ownerUuid)) {
            return AuctionCancellationResult.ALREADY_COMPLETED;
        }
        if (hasBids(auction)) {
            return AuctionCancellationResult.HAS_BIDS;
        }
        String key = auction.getHouseName().toLowerCase();
        if (auctions.remove(key, auction)) {
            if (auction.getAuctionId() != null) {
                auctionsById.remove(auction.getAuctionId(), auction);
            }
            if (save()) {
                return AuctionCancellationResult.SUCCESS;
            }
            auctions.put(key, auction);
            if (auction.getAuctionId() != null) {
                auctionsById.put(auction.getAuctionId(), auction);
            }
            save();
        }
        return AuctionCancellationResult.TRANSACTION_ERROR;
    }

    private boolean isActiveListing(HouseAuction auction, UUID ownerUuid) {
        if (auction == null || ownerUuid == null || auction.getAuctionId() == null
                || auction.getAuctionId().trim().isEmpty() || auction.getHouseName() == null
                || auction.getHouseName().trim().isEmpty() || auction.getSeller() == null
                || !ownerUuid.equals(auction.getSeller()) || auction.getEndAt() <= System.currentTimeMillis()
                || auction.isOwnershipTransferred() || auction.getSettlementState() == null
                || !"ACTIVE".equalsIgnoreCase(auction.getSettlementState())
                || !Double.isFinite(auction.getStartingDiamonds()) || auction.getStartingDiamonds() <= 0
                || !Double.isFinite(auction.getStartingBalance()) || auction.getStartingBalance() < 0
                || auction.getCommittedBids() == null) {
            return false;
        }
        House house = plugin.getHouseManager().get(auction.getHouseName());
        return house != null && house.getType() == HouseType.PREMIUM && ownerUuid.equals(house.getOwner());
    }

    private boolean hasBids(HouseAuction auction) {
        return auction.getHighestBidder() != null || auction.getCommittedBids() == null
                || !auction.getCommittedBids().isEmpty();
    }

    public Collection<HouseAuction> all() {
        return new ArrayList<>(auctions.values());
    }

    public boolean create(HouseAuction auction) {
        if (get(auction.getHouseName()) != null) return false;
        auctions.put(auction.getHouseName().toLowerCase(), auction);
        if (auction.getAuctionId() != null) {
            auctionsById.put(auction.getAuctionId(), auction);
        }
        if (save()) return true;
        auctions.remove(auction.getHouseName().toLowerCase());
        if (auction.getAuctionId() != null) {
            auctionsById.remove(auction.getAuctionId(), auction);
        }
        return false;
    }

    public void remove(HouseAuction auction) {
        if (auction != null && auctions.remove(auction.getHouseName().toLowerCase()) != null) {
            if (auction.getAuctionId() != null) {
                auctionsById.remove(auction.getAuctionId(), auction);
            }
            save();
        }
    }
}