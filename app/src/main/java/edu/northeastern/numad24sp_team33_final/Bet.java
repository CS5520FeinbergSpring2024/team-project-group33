package edu.northeastern.numad24sp_team33_final;

public class Bet {
    private String userId;
    private String assetId;
    private int amount;
    private boolean isHigh;

    public Bet() {
    }

    public Bet(String userId, String assetId, int amount, boolean isHigh) {
        this.userId = userId;
        this.assetId = assetId;
        this.amount = amount;
        this.isHigh = isHigh;
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
}