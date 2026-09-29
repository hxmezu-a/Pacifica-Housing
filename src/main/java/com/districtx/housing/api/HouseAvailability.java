package com.districtx.housing.api;

/** Acquisition availability classification for a house. */
public enum HouseAvailability {
    /** Available for purchase. */
    AVAILABLE,
    /** Already owned. */
    OWNED,
    /** Listed for auction. */
    AUCTION,
    /** Not currently available. */
    UNAVAILABLE
}