package edu.northeastern.numad24sp_team33_final;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bet {
    private String userId;
    private String assetId;
    private int amount;
    private boolean isHigh;
    private String date;
    private boolean claimed;

    public Bet() {
    }

    public Bet(String userId, String assetId, int amount, boolean isHigh, String date) {
        this.userId = userId;
        this.assetId = assetId;
        this.amount = amount;
        this.isHigh = isHigh;
        this.date = date;
        this.claimed = false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isHigh() {
        return isHigh;
    }

    public void setHigh(boolean high) {
        isHigh = high;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // Helper method to get the current date in "YYYY-MM-DD" format
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
}