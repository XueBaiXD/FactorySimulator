package com.bai.xuebai.factorysimulator.model;

import java.util.HashMap;
import java.util.Map;

public class PlacedMachine {
    private String type;
    private int x;
    private int y;
    private int z;
    private String facing = "NORTH";
    private int level = 1;
    private long progress;
    private Map<String, Integer> inventory = new HashMap<>();

    public PlacedMachine() {
    }

    public PlacedMachine(String type, int x, int y, int z, String facing) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public String getFacing() {
        return facing;
    }

    public void setFacing(String facing) {
        this.facing = facing;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public Map<String, Integer> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, Integer> inventory) {
        this.inventory = inventory;
    }
}