package com.districtx.housing.api;

import lombok.Getter;
import com.districtx.housing.api.auction.AuctionStatus;

import java.util.Optional;
import java.util.UUID;

@Getter
public final class AuctionInfo {
    private final String auctionId;
    private final String houseId;
    private final String houseName;
    private final UUID seller;
    private final double startingDiamonds;
    private final double startingBalance;
    private final double currentDiamonds;
    private final double currentBalance;
    private final boolean balanceAllowed;
    private final long endAt;
    private final String settlementState;
    private final UUID highestBidder;
    private final double buyoutPrice;
    private final boolean buyoutEnabled;
    private final int bidCount;
    private final long createdAt;

    public AuctionInfo(String houseId, String houseName, UUID seller, double startingDiamonds,
                       double startingBalance, double currentDiamonds, double currentBalance,
                       boolean balanceAllowed, long endAt, String settlementState, UUID highestBidder) {
        this(null, houseId, houseName, seller, startingDiamonds, startingBalance, currentDiamonds,
                currentBalance, balanceAllowed, endAt, settlementState, highestBidder, 0.0, false,
                highestBidder == null ? 0 : 1, 0L);
    }

    public AuctionInfo(String auctionId, String houseId, String houseName, UUID seller,
                       double startingDiamonds, double startingBalance, double currentDiamonds,
                       double currentBalance, boolean balanceAllowed, long endAt, String settlementState,
                       UUID highestBidder, double buyoutPrice, boolean buyoutEnabled, int bidCount,
                       long createdAt) {
        this.auctionId = auctionId;
        this.houseId = houseId;
        this.houseName = houseName;
        this.seller = seller;
        this.startingDiamonds = startingDiamonds;
        this.startingBalance = startingBalance;
        this.currentDiamonds = currentDiamonds;
        this.currentBalance = currentBalance;
        this.balanceAllowed = balanceAllowed;
        this.endAt = endAt;
        this.settlementState = settlementState;
        this.highestBidder = highestBidder;
        this.buyoutPrice = buyoutPrice;
        this.buyoutEnabled = buyoutEnabled;
        this.bidCount = bidCount;
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return getStatus() == AuctionStatus.ACTIVE;
    }

    public boolean isEnded() {
        return !isActive();
    }

    public boolean hasBids() {
        return highestBidder != null || bidCount > 0;
    }

    public Optional<UUID> getHighestBidderOptional() {
        return Optional.ofNullable(highestBidder);
    }

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
    public String getSettlementState() {
        return settlementState;
    }

    public UUID getOwnerUuid() {
        return seller;
    }

    public String getHouseID() {
        return houseId;
    }
}