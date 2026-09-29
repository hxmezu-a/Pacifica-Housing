package com.districtx.housing.api.auction;

/** Result of an owner-UUID-based auction cancellation attempt. */
public enum AuctionCancellationResult {
    SUCCESS,
    AUCTION_NOT_FOUND,
    NOT_OWNER,
    HAS_BIDS,
    ALREADY_CANCELLED,
    ALREADY_COMPLETED,
    TRANSACTION_ERROR
}