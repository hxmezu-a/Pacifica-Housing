package com.kodari.housing.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class House {
    private final String name;
    private final HouseType type;
    private final double price;
    private UUID owner;
    private final List<HouseDoor> doors = new ArrayList<>();
    private final List<HouseVault> vaults = new ArrayList<>();

    public House(String name, HouseType type, double price) {
        this.name = name;
        this.type = type;
        this.price = price;
    }
}