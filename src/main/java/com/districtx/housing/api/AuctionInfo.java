package com.districtx.housing.api;

import com.districtx.housing.api.auction.AuctionStatus;

import java.util.Optional;
import java.util.UUID;

/** Immutable public snapshot of a house auction. Times are epoch milliseconds. */
public final class AuctionInfo {
    private final String auctionId;
    private final String houseId;
    private final String houseName;
    private final UUID seller;
    private final String sellerName;
    private final double startingDiamonds;
    private final double startingBalance;
    private final double currentDiamonds;
    private final double currentBalance;
    private final boolean balanceAllowed;
    private final long endAt;
    private final String settlementState;
    private final UUID highestBidder;
    private final String highestBidderName;
    private final double buyoutPrice;
    private final boolean buyoutEnabled;
    private final int bidCount;
    private final long createdAt;

    /** Creates an auction snapshot using only UUIDs for participants. */
    public AuctionInfo(String houseId, String houseName, UUID seller, double startingDiamonds,
                       double startingBalance, double currentDiamonds, double currentBalance,
                       boolean balanceAllowed, long endAt, String settlementState, UUID highestBidder) {
        this(null, houseId, houseName, seller, null, startingDiamonds, startingBalance, currentDiamonds,
                currentBalance, balanceAllowed, endAt, settlementState, highestBidder, null, 0.0,
                false, highestBidder == null ? 0 : 1, 0L);
    }

    /** Creates an auction snapshot using only UUIDs for participants. */
    public AuctionInfo(String auctionId, String houseId, String houseName, UUID seller,
                       double startingDiamonds, double startingBalance, double currentDiamonds,
                       double currentBalance, boolean balanceAllowed, long endAt, String settlementState,
                       UUID highestBidder, double buyoutPrice, boolean buyoutEnabled, int bidCount,
                       long createdAt) {
        this(auctionId, houseId, houseName, seller, null, startingDiamonds, startingBalance,
                currentDiamonds, currentBalance, balanceAllowed, endAt, settlementState, highestBidder,
                null, buyoutPrice, buyoutEnabled, bidCount, createdAt);
    }

    /** Creates an auction snapshot including the best-known participant names. */
    public AuctionInfo(String auctionId, String houseId, String houseName, UUID seller, String sellerName,
                       double startingDiamonds, double startingBalance, double currentDiamonds,
                       double currentBalance, boolean balanceAllowed, long endAt, String settlementState,
                       UUID highestBidder, String highestBidderName, double buyoutPrice,
                       boolean buyoutEnabled, int bidCount, long createdAt) {
        this.auctionId = auctionId;
        this.houseId = houseId;
        this.houseName = houseName;
        this.seller = seller;
        this.sellerName = sellerName;
        this.startingDiamonds = startingDiamonds;
        this.startingBalance = startingBalance;
        this.currentDiamonds = currentDiamonds;
        this.currentBalance = currentBalance;
        this.balanceAllowed = balanceAllowed;
        this.endAt = endAt;
        this.settlementState = settlementState;
        this.highestBidder = highestBidder;
        this.highestBidderName = highestBidderName;
        this.buyoutPrice = buyoutPrice;
        this.buyoutEnabled = buyoutEnabled;
        this.bidCount = bidCount;
        this.createdAt = createdAt;
    }

    /** Returns the auction identifier, or null for legacy snapshots without one. */
    public String getAuctionId() {
        return auctionId;
    }

    /** Returns the stable house identifier. */
    public String getHouseId() {
        return houseId;
    }

    /** Returns the display name of the auctioned house. */
    public String getHouseName() {
        return houseName;
    }

    /** Returns the seller UUID. */
    public UUID getSeller() {
        return seller;
    }

    /** Returns the best-known seller name, or null if unavailable. */
    public String getSellerName() {
        return sellerName;
    }

    /** Returns the configured minimum diamond bid. */
    public double getStartingDiamonds() {
        return startingDiamonds;
    }

    /** Returns the configured minimum balance bid. */
    public double getStartingBalance() {
        return startingBalance;
    }

    /** Returns the current diamond bid amount. */
    public double getCurrentDiamonds() {
        return currentDiamonds;
    }

    /** Returns the current balance bid amount. */
    public double getCurrentBalance() {
        return currentBalance;
    }

    /** Returns whether a balance component is allowed in bids. */
    public boolean isBalanceAllowed() {
        return balanceAllowed;
    }

    /** Returns the auction end time in epoch milliseconds. */
    public long getEndAt() {
        return endAt;
    }

    /** Returns the highest bidder UUID, or null when there are no bids. */
    public UUID getHighestBidder() {
        return highestBidder;
    }

    /** Returns the best-known highest bidder name, or null when unavailable. */
    public String getHighestBidderName() {
        return highestBidderName;
    }

    /** Returns the buyout amount. */
    public double getBuyoutPrice() {
        return buyoutPrice;
    }

    /** Returns whether buyout is enabled. */
    public boolean isBuyoutEnabled() {
        return buyoutEnabled;
    }

    /** Returns the number of recorded bids. */
    public int getBidCount() {
        return bidCount;
    }

    /** Returns the auction creation time in epoch milliseconds. */
    public long getCreatedAt() {
        return createdAt;
    }

    /** Returns whether the auction is currently active. */
    public boolean isActive() {
        return getStatus() == AuctionStatus.ACTIVE;
    }

    /** Returns whether the auction is no longer active. */
    public boolean isEnded() {
        return !isActive();
    }

    /** Returns whether at least one bid is recorded. */
    public boolean hasBids() {
        return highestBidder != null || bidCount > 0;
    }

    /** Returns the highest bidder UUID as an optional value. */
    public Optional<UUID> getHighestBidderOptional() {
        return Optional.ofNullable(highestBidder);
    }

    /** Calculates the public auction status from its state and end time. */
    public AuctionStatus getStatus() {
        if ("ACTIVE".equalsIgnoreCase(settlementState) && endAt > System.currentTimeMillis()) {
            return AuctionStatus.ACTIVE;
        }
        if ("SETTLING".equalsIgnoreCase(settlementState)) {
            return AuctionStatus.SETTLING;
        }
        if ("CANCELLED".equalsIgnoreCase(settlementState) || "CANCELED".equalsIgnoreCase(settlementState)) {
            return AuctionStatus.CANCELLED;
        }
        if ("SETTLED".equalsIgnoreCase(settlementState) || "COMPLETED".equalsIgnoreCase(settlementState)
                || endAt <= System.currentTimeMillis()) {
            return AuctionStatus.COMPLETED;
        }
        return AuctionStatus.UNKNOWN;
    }

    @Deprecated
    /** @deprecated Use {@link #getStatus()}. */
    public String getSettlementState() {
        return settlementState;
    }

    /** Returns the seller UUID. */
    public UUID getOwnerUuid() {
        return seller;
    }

    /** Returns the stable house identifier. */
    public String getHouseID() {
        return houseId;
    }
}