package com.bai.xuebai.factorysimulator.storage;

import com.bai.xuebai.factorysimulator.model.FactoryProfile;

import java.util.Collection;
import java.util.UUID;

public interface FactoryStorage {
    void load();
    void saveAll();
    void close();
    FactoryProfile getOrCreate(UUID uuid, String playerName);
    FactoryProfile getById(String id);
    FactoryProfile getByName(String name);
    Collection<FactoryProfile> getAll();
    void save(FactoryProfile profile);
    boolean exists(UUID uuid);
}