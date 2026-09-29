package com.districtx.housing.api;

import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
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

    public HouseInfo(String id, String name, double price, HouseType type, UUID ownerUuid, String ownerName,
                     Location location, Location teleportLocation, String region, HouseStatus status,
                     HouseAvailability availability, List<HouseDoorInfo> doors, AuctionInfo auction) {
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
    }

    public String getHouseId() {
        return id;
    }

    public String getHouseName() {
        return name;
    }

    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public boolean isOwned() {
        return ownerUuid != null;
    }

    public boolean isAvailable() {
        return availability == HouseAvailability.AVAILABLE;
    }

    public boolean isNormal() {
        return type == HouseType.REGULAR;
    }

    public boolean isPremium() {
        return type == HouseType.PREMIUM;
    }

    public boolean isLuxury() {
        return type == HouseType.LUXURY;
    }

    public boolean hasActiveAuction() {
        return auction != null && auction.isActive();
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    public Location getTeleportLocation() {
        return teleportLocation == null ? null : teleportLocation.clone();
    }
}