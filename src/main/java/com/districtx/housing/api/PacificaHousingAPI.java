package com.districtx.housing.api;

import com.districtx.housing.api.auction.AuctionService;
import com.districtx.housing.api.house.HouseService;
import com.districtx.housing.api.vault.VaultService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable service entry point for integrations with Pacifica-Housing.
 * Query methods access live Bukkit state and should be called on the server thread.
 */
public interface PacificaHousingAPI {
    /**
     * Gets the registered API service, if Pacifica-Housing is enabled.
     *
     * @return the registered API, or an empty value when it is unavailable
     */
    static Optional<PacificaHousingAPI> get() {
        if (Bukkit.getServer() == null) {
            return Optional.empty();
        }
        RegisteredServiceProvider<PacificaHousingAPI> registration =
                Bukkit.getServicesManager().getRegistration(PacificaHousingAPI.class);
        return Optional.ofNullable(registration == null ? null : registration.getProvider());
    }

    /**
     * Checks whether the API service is currently registered and available.
     *
     * @return true when Pacifica-Housing is enabled and its API is registered
     */
    static boolean isAvailable() {
        return get().isPresent();
    }

    /** Returns read-only house queries backed by immutable snapshots. */
    HouseService houses();

    /** Returns auction snapshots and typed cancellation operations. */
    AuctionService auctions();

    /** Returns vault snapshots with copied contents, never live inventory objects. */
    VaultService vaults();

    /** @deprecated Use {@link #houses()} and its optional-returning methods. */
    @Deprecated
    HouseInfo getHouse(String name);

    /** @deprecated Use {@link #houses()} and its optional-returning methods. */
    @Deprecated
    HouseInfo getHouseById(String id);

    /** @deprecated Use {@link HouseService#getHouseAt(Location)}. */
    @Deprecated
    HouseInfo getHouseAt(Location location);

    /** @deprecated Use {@link #houses()}. */
    @Deprecated
    Collection<HouseInfo> getHouses();

    /** @deprecated Filter {@link #houses()} snapshots by {@link HouseInfo#isAvailable()}. */
    @Deprecated
    Collection<HouseInfo> getAvailableHouses();

    /** @deprecated Use {@link HouseService#getHousesByOwner(UUID)}. */
    @Deprecated
    Collection<HouseInfo> getHousesByOwner(UUID owner);

    /** @deprecated Use {@link HouseService#getHousesByOwner(UUID)}. */
    @Deprecated
    HouseInfo getOwnedHouse(UUID owner);

    /**
     * Gets the house currently tracked for a player, or null when they are outside.
     *
     * @param player player to query
     * @return the house snapshot, or null when the player is not inside a tracked house
     */
    HouseInfo getHouseFor(Player player);

    /**
     * Checks whether Pacifica-Housing currently tracks the player inside a house.
     *
     * @param player player to query
     * @return true if the player is tracked inside a house
     */
    boolean isPlayerInside(Player player);

    /** @deprecated Use {@link com.districtx.housing.api.auction.AuctionService#getAuction(String)}. */
    @Deprecated
    AuctionInfo getAuction(String houseNameOrId);

    /** @deprecated Use {@link #auctions()}. */
    @Deprecated
    Collection<AuctionInfo> getAuctions();

    /** @deprecated Use {@link com.districtx.housing.api.auction.AuctionService#getActiveListings(UUID)}. */
    @Deprecated
    Collection<AuctionInfo> getActiveListings(UUID ownerUuid);

    /** @deprecated Use {@link com.districtx.housing.api.auction.AuctionService#hasBids(String)}. */
    @Deprecated
    boolean hasBids(String houseNameOrAuctionId);

    /** @deprecated Use {@link com.districtx.housing.api.auction.AuctionService#canCancel(String, UUID)}. */
    @Deprecated
    boolean canCancelAuction(String houseNameOrAuctionId, UUID ownerUuid);

    /** @deprecated Use {@link com.districtx.housing.api.auction.AuctionService#cancelAuction(String, UUID)}. */
    @Deprecated
    boolean cancelAuction(String houseNameOrAuctionId, UUID ownerUuid);
}