package com.kodari.housing.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class HouseDoor {
    private final String id;
    private final int number;
    private final Location door;
    private Location outside;
    private Location inside;

    public HouseDoor(String id, int number, Location door) {
        this.id = id;
        this.number = number;
        this.door = door;
    }
}