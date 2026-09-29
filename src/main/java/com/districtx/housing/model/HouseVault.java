package com.districtx.housing.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class HouseVault {
    public static final int INVENTORY_SIZE = 54;
    private final String id;
    private final Location location;
    private ItemStack[] contents = new ItemStack[INVENTORY_SIZE];

    public HouseVault(String id, Location location) {
        this.id = id;
        this.location = location;
    }
}