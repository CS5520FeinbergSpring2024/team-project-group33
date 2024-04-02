package edu.northeastern.numad24sp_team33_final;

public class User {
    public String email;
    public String id;
    public int points;
    public String lastBonusDate;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String id, int points, String lastBonusDate) {
        this.email = email;
        this.id = id;
        this.points = points;
        this.lastBonusDate = lastBonusDate;
    }
}
