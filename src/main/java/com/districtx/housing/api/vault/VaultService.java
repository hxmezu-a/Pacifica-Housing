package com.districtx.housing.api.vault;

import com.districtx.housing.api.VaultInfo;

import java.util.List;
import java.util.Optional;

/** Read-only vault information snapshots; returned items are copies and do not mutate storage. */
public interface VaultService {
    /**
     * Gets the number of configured vaults for a house.
     *
     * @param houseId stable house ID or case-insensitive house name
     * @return the number of vaults, or zero if the house does not exist
     */
    int getVaultCount(String houseId);

    /**
     * Finds a vault by its one-based house-local number.
     *
     * @param houseId stable house ID or case-insensitive house name
     * @param vaultNumber one-based number shown in the vault title
     * @return a snapshot, or empty if the house or vault does not exist
     */
    Optional<VaultInfo> getVault(String houseId, int vaultNumber);

    /**
     * Gets immutable vault snapshots for a house.
     *
     * @param houseId stable house ID or case-insensitive house name
     * @return an immutable list, empty if the house does not exist
     */
    List<VaultInfo> getVaults(String houseId);

    /**
     * Checks whether a house-local vault number currently exists.
     *
     * @param houseId stable house ID or case-insensitive house name
     * @param vaultNumber one-based vault number
     * @return true if that vault exists
     */
    boolean vaultExists(String houseId, int vaultNumber);

    /**
     * Returns whether the house has at least one vault.
     *
     * @param houseId stable house ID or case-insensitive house name
     * @return true if the house has a vault
     */
    default boolean hasVaults(String houseId) {
        return getVaultCount(houseId) > 0;
    }
}