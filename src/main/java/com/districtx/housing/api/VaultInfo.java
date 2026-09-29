package com.districtx.housing.api;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/** Immutable metadata and copied contents for one house vault. */
public final class VaultInfo {
    private final String id;
    private final String houseId;
    private final String houseName;
    private final java.util.UUID ownerUuid;
    private final int number;
    private final String title;
    private final Location location;
    private final ItemStack[] contents;

    /** Creates a vault snapshot; all mutable Bukkit values are copied. */
    public VaultInfo(String id, String houseId, String houseName, java.util.UUID ownerUuid, int number,
                     String title, Location location, ItemStack[] contents) {
        this.id = id;
        this.houseId = houseId;
        this.houseName = houseName;
        this.ownerUuid = ownerUuid;
        this.number = number;
        this.title = title;
        this.location = location == null ? null : location.clone();
        this.contents = copyContents(contents);
    }

    /** Returns the vault's stable identifier. */
    public String getId() {
        return id;
    }

    /** Returns the stable ID of the house containing this vault. */
    public String getHouseId() {
        return houseId;
    }

    /** Returns the display name of the house containing this vault. */
    public String getHouseName() {
        return houseName;
    }

    /** Returns the UUID of the house owner, or null when the house is unowned. */
    public java.util.UUID getOwnerUuid() {
        return ownerUuid;
    }

    /** Returns this vault's one-based number within its house. */
    public int getNumber() {
        return number;
    }

    /** Returns the rendered legacy-color title used by the vault inventory. */
    public String getTitle() {
        return title;
    }

    /** Returns the linked physical container location, or null if it is unavailable. */
    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    /**
     * Returns a copied contents snapshot. Mutating the returned array or items does not change
     * the stored vault; call on the server thread because Bukkit item stacks are not thread-safe.
     */
    public ItemStack[] getContents() {
        return copyContents(contents);
    }

    private static ItemStack[] copyContents(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int slot = 0; slot < source.length; slot++) {
            copy[slot] = source[slot] == null ? null : source[slot].clone();
        }
        return copy;
    }
}