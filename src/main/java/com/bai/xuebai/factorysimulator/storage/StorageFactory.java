package com.bai.xuebai.factorysimulator.storage;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.config.StorageType;
import com.bai.xuebai.factorysimulator.storage.jdbc.JdbcFactoryStorage;
import com.bai.xuebai.factorysimulator.storage.yaml.YamlFactoryStorage;

public final class StorageFactory {
    private StorageFactory() {}

    public static FactoryStorage create(FactorySimulator plugin, PluginConfig config) {
        StorageType type = config.getStorageType();
        if (type == StorageType.SQLITE || type == StorageType.MYSQL) {
            return new JdbcFactoryStorage(plugin, config);
        }
        return new YamlFactoryStorage(plugin);
    }
}