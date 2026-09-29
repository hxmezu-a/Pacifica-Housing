package com.districtx.housing.api.vault;

import java.util.List;

/** Read-only vault metadata; inventory contents and Bukkit inventories remain internal. */
public interface VaultService {
    /** Returns zero if the house does not exist; inventory contents and locations are not exposed. */
    int getVaultCount(String houseId);

    default boolean hasVaults(String houseId) {
        return getVaultCount(houseId) > 0;
    }
}