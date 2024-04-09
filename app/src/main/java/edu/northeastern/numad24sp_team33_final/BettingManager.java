package edu.northeastern.numad24sp_team33_final;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.util.Log;

public class BettingManager {
    private DatabaseReference databaseReference;

    public BettingManager() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public void placeBet(String assetId, boolean isHigh, String userId, int amount, Runnable onSuccessUpdateUI) {
        Bet bet = new Bet(userId, assetId, amount, isHigh);
        String betId = databaseReference.child("bets").push().getKey();
        databaseReference.child("bets").child(betId).setValue(bet).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateUserPoints(userId, -amount, onSuccessUpdateUI);
            } else {
                Log.e("Firebase", "Failed to place bet: " + task.getException().getMessage());
            }
        });
    }

    private void updateUserPoints(String userId, int pointsChange, Runnable onSuccessUpdateUI) {
        DatabaseReference userRef = databaseReference.child("users").child(userId).child("points");
        userRef.get().addOnSuccessListener(snapshot -> {
            Integer currentPoints = snapshot.getValue(Integer.class);
            if (currentPoints != null) {
                userRef.setValue(currentPoints + pointsChange).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccessUpdateUI.run();
                    }
                });
            }
        });
    }
}