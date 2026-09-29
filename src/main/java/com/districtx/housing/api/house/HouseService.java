package com.districtx.housing.api.house;

import com.districtx.housing.api.HouseInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only house queries. Ownership is identified by UUID, not player name. */
public interface HouseService {
    /** Resolves a house by its stable ID or case-insensitive house name; empty means not found. */
    Optional<HouseInfo> getHouse(String houseId);

    /** Returns an immutable list of immutable house snapshots. */
    List<HouseInfo> getHouses();

    /** Returns immutable snapshots for houses owned by the given UUID. */
    List<HouseInfo> getHousesByOwner(UUID ownerUuid);

    /** Tests ownership without depending on a player name or online session. */
    boolean ownsHouse(UUID playerUuid, String houseId);

    /** Returns false when the house does not exist. */
    boolean isAvailable(String houseId);

    /** Returns false when the house does not exist. */
    boolean isPremium(String houseId);

    /** Returns false when the house does not exist. */
    boolean isLuxury(String houseId);

    /** Finds a house whose configured door occupies the given block. */
    Optional<HouseInfo> getHouseAt(Location location);

    /**
     * Begins the existing owner-only teleport flow to a player's house.
     * This does not bypass Pacifica-Housing's ownership or transition rules.
     *
     * @param player online player requesting the teleport
     * @param houseId stable house ID or case-insensitive house name
     * @return true if the teleport flow was accepted, otherwise false
     */
    boolean teleportToHouse(Player player, String houseId);
}