package com.districtx.housing.api;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Immutable snapshot of public house data. Bukkit locations returned by this model are cloned. */
public final class HouseInfo {
    private final String id;
    private final String name;
    private final double price;
    private final HouseType type;
    private final UUID ownerUuid;
    private final String ownerName;
    private final Location location;
    private final Location teleportLocation;
    private final String region;
    private final HouseStatus status;
    private final HouseAvailability availability;
    private final List<HouseDoorInfo> doors;
    private final AuctionInfo auction;
    private final int vaultCount;

    /** Creates a snapshot without vault-count metadata for source compatibility. */
    public HouseInfo(String id, String name, double price, HouseType type, UUID ownerUuid, String ownerName,
                     Location location, Location teleportLocation, String region, HouseStatus status,
                     HouseAvailability availability, List<HouseDoorInfo> doors, AuctionInfo auction) {
        this(id, name, price, type, ownerUuid, ownerName, location, teleportLocation, region, status,
                availability, doors, auction, 0);
    }

    /**
     * Creates an immutable house snapshot including its vault count.
     *
     * @param id stable house identifier
     * @param name display name
     * @param price configured price
     * @param type public house type
     * @param ownerUuid owner UUID, or null when unowned
     * @param ownerName last known owner name, or null when unavailable
     * @param location configured house location, or null when unavailable
     * @param teleportLocation teleport destination, or null when unavailable
     * @param region region identifier, or null when the plugin has no region data
     * @param status current house status
     * @param availability current availability
     * @param doors immutable door snapshots
     * @param auction active or stored auction snapshot, or null when absent
     * @param vaultCount number of configured vaults
     */
    public HouseInfo(String id, String name, double price, HouseType type, UUID ownerUuid, String ownerName,
                     Location location, Location teleportLocation, String region, HouseStatus status,
                     HouseAvailability availability, List<HouseDoorInfo> doors, AuctionInfo auction,
                     int vaultCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.location = location == null ? null : location.clone();
        this.teleportLocation = teleportLocation == null ? null : teleportLocation.clone();
        this.region = region;
        this.status = status;
        this.availability = availability;
        this.doors = doors == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(doors));
        this.auction = auction;
        this.vaultCount = Math.max(0, vaultCount);
    }

    /** Returns the stable house identifier. */
    public String getId() {
        return id;
    }

    /** Returns the house display name. */
    public String getName() {
        return name;
    }

    /** Returns the configured house price. */
    public double getPrice() {
        return price;
    }

    /** Returns the public house type. */
    public HouseType getType() {
        return type;
    }

    /** Returns the owner's UUID, or null when unowned. */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    /** Returns the last known owner name, or null when unavailable. */
    public String getOwnerName() {
        return ownerName;
    }

    /** Returns the nullable region identifier. */
    public String getRegion() {
        return region;
    }

    /** Returns the current house status. */
    public HouseStatus getStatus() {
        return status;
    }

    /** Returns the current availability classification. */
    public HouseAvailability getAvailability() {
        return availability;
    }

    /** Returns an immutable list of door snapshots. */
    public List<HouseDoorInfo> getDoors() {
        return doors;
    }

    /** Returns the auction snapshot, or null when no auction is recorded. */
    public AuctionInfo getAuction() {
        return auction;
    }

    /** Returns the stable house identifier. */
    public String getHouseId() {
        return id;
    }

    /** Returns the house display name. */
    public String getHouseName() {
        return name;
    }

    /** Returns the owner's UUID, or null when unowned. */
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    /** Returns whether the house has an owner. */
    public boolean isOwned() {
        return ownerUuid != null;
    }

    /** Returns whether the house is currently available for purchase. */
    public boolean isAvailable() {
        return availability == HouseAvailability.AVAILABLE;
    }

    /** Returns whether this is a regular house. */
    public boolean isNormal() {
        return type == HouseType.REGULAR;
    }

    /** Returns whether this is a premium house. */
    public boolean isPremium() {
        return type == HouseType.PREMIUM;
    }

    /** Returns whether this is a luxury house. */
    public boolean isLuxury() {
        return type == HouseType.LUXURY;
    }

    /** Returns whether this house currently has an active auction. */
    public boolean hasActiveAuction() {
        return auction != null && auction.isActive();
    }

    /** Returns the number of vaults associated with this house. */
    public int getVaultCount() {
        return vaultCount;
    }

    /** Returns a copy of the configured house location, or null when unavailable. */
    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    /** Returns a copy of the teleport destination, or null when unavailable. */
    public Location getTeleportLocation() {
        return teleportLocation == null ? null : teleportLocation.clone();
    }
}