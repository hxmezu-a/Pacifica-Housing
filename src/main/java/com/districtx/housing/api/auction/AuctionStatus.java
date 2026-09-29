package com.districtx.housing.api.auction;

/** Public state of an auction. */
public enum AuctionStatus {
    /** Auction accepts bids. */
    ACTIVE,
    /** Auction settlement is in progress. */
    SETTLING,
    /** Auction was cancelled. */
    CANCELLED,
    /** Auction was completed or expired. */
    COMPLETED,
    /** State could not be mapped to a known status. */
    UNKNOWN
}