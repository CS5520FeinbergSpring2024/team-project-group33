package edu.northeastern.numad24sp_team33_final;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class UserPointsUpdater {
    private DatabaseReference mDatabase;

    public UserPointsUpdater() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void updatePointsByEmail(String email, int pointsChange, PointsUpdateListener listener) {
        Query query = mDatabase.child("users").orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            int newPoints = user.points + pointsChange;
                            userSnapshot.getRef().child("points").setValue(newPoints)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(listener::onFailure);
                        }
                    }
                } else {
                    listener.onFailure(new Exception("User not found"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure(new Exception(databaseError.toException()));
            }
        });
    }
}


