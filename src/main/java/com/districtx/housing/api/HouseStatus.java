package com.districtx.housing.api;

/** Operational status of a configured house. */
public enum HouseStatus {
    /** Configured and available for purchase. */
    AVAILABLE,
    /** Currently owned by a player. */
    OWNED,
    /** Currently listed in an auction. */
    AUCTION,
    /** Not available for acquisition. */
    UNAVAILABLE,
    /** Missing a required entrance location or configuration. */
    NOT_READY
}