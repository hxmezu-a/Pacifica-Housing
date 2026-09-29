package com.districtx.housing.api;

import lombok.Getter;
import org.bukkit.Location;

@Getter
public final class HouseDoorInfo {
    private final String id;
    private final int number;
    private final Location doorLocation;
    private final Location outsideLocation;
    private final Location insideLocation;

    public HouseDoorInfo(String id, int number, Location doorLocation, Location outsideLocation,
                         Location insideLocation) {
        this.id = id;
        this.number = number;
        this.doorLocation = doorLocation == null ? null : doorLocation.clone();
        this.outsideLocation = outsideLocation == null ? null : outsideLocation.clone();
        this.insideLocation = insideLocation == null ? null : insideLocation.clone();
    }

    public Location getDoorLocation() {
        return doorLocation == null ? null : doorLocation.clone();
    }

    public Location getOutsideLocation() {
        return outsideLocation == null ? null : outsideLocation.clone();
    }

    public Location getInsideLocation() {
        return insideLocation == null ? null : insideLocation.clone();
    }

    public boolean isConfigured() {
        return doorLocation != null && outsideLocation != null && insideLocation != null;
    }
}