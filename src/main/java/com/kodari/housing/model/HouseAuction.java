package com.kodari.housing.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class HouseAuction {
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

    public HouseAuction(String houseName, UUID seller, double startingDiamonds,
                        double startingBalance, boolean balanceAllowed, long endAt) {
        this.houseName = houseName;
        this.seller = seller;
        this.startingDiamonds = startingDiamonds;
        this.startingBalance = startingBalance;
        this.balanceAllowed = balanceAllowed;
        this.endAt = endAt;
        this.currentDiamonds = startingDiamonds;
        this.currentBalance = startingBalance;
    }
}