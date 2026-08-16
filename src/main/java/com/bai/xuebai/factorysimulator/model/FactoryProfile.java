package com.bai.xuebai.factorysimulator.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactoryProfile {
    private String playerId;
    private String playerName;
    private String worldName;
    private String factoryName;
    private boolean created;
    private long createdAt;
    private long lastOnlineAt;
    private long lastClaimAt;
    private int level;
    private int plotSize;
    private double money;
    private double offlineStoredMoney;
    private int workers;
    private int machines;
    private Set<String> achievements = new HashSet<>();
    private List<String> unlockedMachines = new ArrayList<>();
    private List<PlacedMachine> layout = new ArrayList<>();

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(long lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public long getLastClaimAt() {
        return lastClaimAt;
    }

    public void setLastClaimAt(long lastClaimAt) {
        this.lastClaimAt = lastClaimAt;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPlotSize() {
        return plotSize;
    }

    public void setPlotSize(int plotSize) {
        this.plotSize = plotSize;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public double getOfflineStoredMoney() {
        return offlineStoredMoney;
    }

    public void setOfflineStoredMoney(double offlineStoredMoney) {
        this.offlineStoredMoney = offlineStoredMoney;
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public int getMachines() {
        return machines;
    }

    public void setMachines(int machines) {
        this.machines = machines;
    }

    public Set<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(Set<String> achievements) {
        this.achievements = achievements;
    }

    public List<String> getUnlockedMachines() {
        return unlockedMachines;
    }

    public void setUnlockedMachines(List<String> unlockedMachines) {
        this.unlockedMachines = unlockedMachines;
    }

    public List<PlacedMachine> getLayout() {
        return layout;
    }

    public void setLayout(List<PlacedMachine> layout) {
        this.layout = layout;
    }
}