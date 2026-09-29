package com.districtx.housing.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class AuctionCreationSession {
    private final UUID owner;
    private final String houseName;
    private BigDecimal minimumDiamondBid;
    private BigDecimal startingBid;
    private BigDecimal buyoutPrice;
    private BigDecimal listingFee;
    private boolean diamondBidConfigured;
    private boolean startingBidConfigured;
    private boolean listingFeeConfigured;
    private boolean buyoutConfigured;
    private boolean creatingAuction;

    public AuctionCreationSession(UUID owner, String houseName) {
        this.owner = owner;
        this.houseName = houseName;
    }
}