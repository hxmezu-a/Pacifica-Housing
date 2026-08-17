package com.kodari.housing.model;

public enum HouseType {
    REGULAR,
    PREMIUM,
    LUXURY;

    public static HouseType from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}