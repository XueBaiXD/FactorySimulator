package com.bai.xuebai.factorysimulator.config;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    public static StorageType fromString(String value) {
        if (value == null) {
            return YAML;
        }
        try {
            return StorageType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return YAML;
        }
    }
}