package com.districtx.housing.api;

import org.bukkit.Location;

/** Immutable snapshot of a configured house entrance and its locations. */
public final class HouseDoorInfo {
    private final String id;
    private final int number;
    private final Location doorLocation;
    private final Location outsideLocation;
    private final Location insideLocation;

    /** Creates a door snapshot with copied Bukkit locations. */
    public HouseDoorInfo(String id, int number, Location doorLocation, Location outsideLocation,
                         Location insideLocation) {
        this.id = id;
        this.number = number;
        this.doorLocation = doorLocation == null ? null : doorLocation.clone();
        this.outsideLocation = outsideLocation == null ? null : outsideLocation.clone();
        this.insideLocation = insideLocation == null ? null : insideLocation.clone();
    }

    /** Returns the stable door identifier. */
    public String getId() {
        return id;
    }

    /** Returns the one-based door number. */
    public int getNumber() {
        return number;
    }

    /** Returns a copy of the door block location, or null if unavailable. */
    public Location getDoorLocation() {
        return doorLocation == null ? null : doorLocation.clone();
    }

    /** Returns a copy of the outside location, or null if unavailable. */
    public Location getOutsideLocation() {
        return outsideLocation == null ? null : outsideLocation.clone();
    }

    /** Returns a copy of the inside location, or null if unavailable. */
    public Location getInsideLocation() {
        return insideLocation == null ? null : insideLocation.clone();
    }

    /** Returns whether all three locations are configured. */
    public boolean isConfigured() {
        return doorLocation != null && outsideLocation != null && insideLocation != null;
    }
}