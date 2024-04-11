package edu.northeastern.numad24sp_team33_final;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class LeaderboardEntry {
    private String name;
    private int points;

    public LeaderboardEntry(String name, int points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }
}
