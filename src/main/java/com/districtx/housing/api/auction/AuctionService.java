package com.districtx.housing.api.auction;

import com.districtx.housing.api.AuctionInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only auction queries and owner-UUID-based cancellation. */
public interface AuctionService {
    /** Resolves an auction by its auction ID or house ID; empty means not found. */
    Optional<AuctionInfo> getAuction(String auctionIdOrHouseId);

    /** Returns immutable snapshots of all stored auctions, including ended auctions. */
    List<AuctionInfo> getAuctions();

    /** Returns only auctions currently active according to server state. */
    List<AuctionInfo> getActiveAuctions();

    /** Returns active listings owned by this UUID. */
    List<AuctionInfo> getActiveListings(UUID ownerUuid);

    /** Returns false when the auction does not exist. */
    boolean hasBids(String auctionIdOrHouseId);

    /** Checks whether the owner can currently cancel this listing. */
    boolean canCancel(String auctionIdOrHouseId, UUID ownerUuid);

    /** Attempts cancellation atomically and reports the reason if it cannot succeed. */
    AuctionCancellationResult cancelAuction(String auctionIdOrHouseId, UUID ownerUuid);
}