package com.districtx.housing.api;

import com.districtx.housing.api.auction.AuctionService;
import com.districtx.housing.api.house.HouseService;
import com.districtx.housing.api.vault.VaultService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/** Stable, read-only service entry point for integrations with Pacifica-Housing. */
public interface PacificaHousingAPI {
    /** Returns read-only house queries backed by immutable snapshots. */
    HouseService houses();

    /** Returns auction queries and typed cancellation operations. */
    AuctionService auctions();

    /** Returns read-only vault metadata without exposing inventories or storage objects. */
    VaultService vaults();

    @Deprecated
    HouseInfo getHouse(String name);

    @Deprecated
    HouseInfo getHouseById(String id);

    @Deprecated
    HouseInfo getHouseAt(Location location);

    @Deprecated
    Collection<HouseInfo> getHouses();

    @Deprecated
    Collection<HouseInfo> getAvailableHouses();

    @Deprecated
    Collection<HouseInfo> getHousesByOwner(UUID owner);

    @Deprecated
    HouseInfo getOwnedHouse(UUID owner);

    HouseInfo getHouseFor(Player player);

    boolean isPlayerInside(Player player);

    @Deprecated
    AuctionInfo getAuction(String houseNameOrId);

    @Deprecated
    Collection<AuctionInfo> getAuctions();

    @Deprecated
    Collection<AuctionInfo> getActiveListings(UUID ownerUuid);

    @Deprecated
    boolean hasBids(String houseNameOrAuctionId);

    @Deprecated
    boolean canCancelAuction(String houseNameOrAuctionId, UUID ownerUuid);

    @Deprecated
    boolean cancelAuction(String houseNameOrAuctionId, UUID ownerUuid);
}