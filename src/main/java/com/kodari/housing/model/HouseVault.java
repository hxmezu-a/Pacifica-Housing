package com.kodari.housing.model;

import lombok.Getter;
import org.bukkit.Location;

@Getter
public class HouseVault {
    private final String id;
    private final Location location;

    public HouseVault(String id, Location location) {
        this.id = id;
        this.location = location;
    }
}