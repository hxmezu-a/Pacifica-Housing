package com.districtx.housing.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class HouseAuction {
    private final String auctionId;
    private final String houseName;
    private final UUID seller;
    private final double startingDiamonds;
    private final double startingBalance;
    private final boolean balanceAllowed;
    private final long endAt;
    private double currentDiamonds;
    private double currentBalance;
    private UUID highestBidder;
    private Map<String, CommittedBid> committedBids = new LinkedHashMap<>();
    private String settlementState = "ACTIVE";
    private boolean sellerDiamondsPaid;
    private boolean sellerBalancePaid;
    private boolean ownershipTransferred;
    private boolean buyoutEnabled;
    private double buyoutPrice;
    private double listingFee;
    private double listingFeePercentage;
    private String sellerRank = "default";
    private final long createdAt;
    private boolean listingFeeRefunded;
    private boolean listingFeePaid;

    public HouseAuction(String houseName, UUID seller, double startingDiamonds,
                        double startingBalance, boolean balanceAllowed, long endAt) {
        this(UUID.randomUUID().toString(), houseName, seller, startingDiamonds, startingBalance,
                balanceAllowed, endAt, System.currentTimeMillis());
    }

    public HouseAuction(String auctionId, String houseName, UUID seller, double startingDiamonds,
                        double startingBalance, boolean balanceAllowed, long endAt, long createdAt) {
        this.auctionId = auctionId;
        this.houseName = houseName;
        this.seller = seller;
        this.startingDiamonds = startingDiamonds;
        this.startingBalance = startingBalance;
        this.balanceAllowed = balanceAllowed;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.currentDiamonds = 0;
        this.currentBalance = startingBalance;
    }
}