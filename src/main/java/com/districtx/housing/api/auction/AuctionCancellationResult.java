package com.districtx.housing.api.auction;

/** Result of an owner-UUID-based auction cancellation attempt. */
public enum AuctionCancellationResult {
    /** Cancellation succeeded and was persisted. */
    SUCCESS,
    /** No matching auction exists. */
    AUCTION_NOT_FOUND,
    /** The supplied UUID does not own this listing. */
    NOT_OWNER,
    /** Existing bids prevent cancellation. */
    HAS_BIDS,
    /** The auction is already cancelled. */
    ALREADY_CANCELLED,
    /** The auction has completed. */
    ALREADY_COMPLETED,
    /** The change could not be persisted safely. */
    TRANSACTION_ERROR
}