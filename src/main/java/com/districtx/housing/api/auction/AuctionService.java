package com.districtx.housing.api.auction;

import com.districtx.housing.api.AuctionInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Auction snapshots and owner-UUID-based cancellation operations. */
public interface AuctionService {
    /**
     * Resolves an auction by its auction ID or house ID.
     *
     * @param auctionIdOrHouseId auction identifier or house identifier/name
     * @return the auction snapshot, or empty when not found
     */
    Optional<AuctionInfo> getAuction(String auctionIdOrHouseId);

    /** Returns immutable snapshots of all stored auctions, including ended auctions. */
    List<AuctionInfo> getAuctions();

    /** Returns only auctions currently active according to server state. */
    List<AuctionInfo> getActiveAuctions();

    /** Returns active listings owned by this UUID. */
    List<AuctionInfo> getActiveListings(UUID ownerUuid);

    /**
     * Checks whether an auction has recorded bids.
     *
     * @param auctionIdOrHouseId auction identifier or house identifier/name
     * @return true if at least one bid is recorded
     */
    boolean hasBids(String auctionIdOrHouseId);

    /**
     * Checks whether the owner can currently cancel this listing.
     *
     * @param auctionIdOrHouseId auction identifier or house identifier/name
     * @param ownerUuid expected seller UUID
     * @return true if cancellation is currently permitted
     */
    boolean canCancel(String auctionIdOrHouseId, UUID ownerUuid);

    /**
     * Attempts cancellation and reports why it could not succeed.
     *
     * @param auctionIdOrHouseId auction identifier or house identifier/name
     * @param ownerUuid expected seller UUID
     * @return the outcome of the cancellation attempt
     */
    AuctionCancellationResult cancelAuction(String auctionIdOrHouseId, UUID ownerUuid);
}