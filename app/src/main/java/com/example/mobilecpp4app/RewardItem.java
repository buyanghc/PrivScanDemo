package com.example.mobilecpp4app;

public class RewardItem {
    private String name;
    private int points;
    private int imageResource;

    public RewardItem(String name, int points, int imageResource) {
        this.name = name;
        this.points = points;
        this.imageResource = imageResource;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public int getImageResource() {
        return imageResource;
    }
}