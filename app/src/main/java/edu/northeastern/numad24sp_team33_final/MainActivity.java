package edu.northeastern.numad24sp_team33_final;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity;

public class MainActivity extends AppCompatActivity {
    private TextView userEmailTextView, userIdTextView, userPointsTextView, bonusMessageTextView;
    private Button logoutButton, reg, login, testPlusOneBtn, testMinusOneBtn, claimBonusButton, leaderboardButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        userEmailTextView = findViewById(R.id.userEmailTextView);
        userIdTextView = findViewById(R.id.userIdTextView);
        userPointsTextView = findViewById(R.id.userPointsTextView);

        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logoutUser());

        reg = findViewById(R.id.toRegBtn);
        reg.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        login = findViewById(R.id.toLoginBtn);
        login.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
//        testPlusOneBtn = findViewById(R.id.addOneBtn);
//        testPlusOneBtn.setOnClickListener(v -> {
//            addOne(mAuth.getCurrentUser());
//        });
//
//        testMinusOneBtn = findViewById(R.id.minusOneBtn);
//        testMinusOneBtn.setOnClickListener(v -> {
//            minusOne(mAuth.getCurrentUser());
//        });
        bonusMessageTextView = findViewById(R.id.bonusMessageTextView);
        claimBonusButton = findViewById(R.id.claimBonusButton);

        claimBonusButton.setOnClickListener(v -> {
            claimDailyBonus(new PointsUpdateListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        // Update the UI to reflect the new points and hide the bonus button
                        updateUI(mAuth.getCurrentUser());
                        bonusMessageTextView.setText("Get bonus next day.");
                        claimBonusButton.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onFailure(Exception exception) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        });

        leaderboardButton = findViewById(R.id.leaderboardButton);
        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        Button amazonButton = findViewById(R.id.amazonButton);
        amazonButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AssetStatusActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(AssetStatusActivity.TICKER_SYMBOL_KEY, "AMAZ");
            intent.putExtras(bundle);
            startActivity(intent);
        });

        Button appleButton = findViewById(R.id.appleButton);
        appleButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AssetStatusActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(AssetStatusActivity.TICKER_SYMBOL_KEY, "AAPL");
            intent.putExtras(bundle);
            startActivity(intent);
        });

        Button teslaButton = findViewById(R.id.teslaButton);
        teslaButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AssetStatusActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(AssetStatusActivity.TICKER_SYMBOL_KEY, "TSLA");
            intent.putExtras(bundle);
            startActivity(intent);
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        checkDailyBonusEligibility(mAuth.getCurrentUser());
        updateUI(currentUser);
    }
    private void checkDailyBonusEligibility(FirebaseUser user) {
        if (user != null) {
            mDatabase.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User userInfo = dataSnapshot.getValue(User.class);
                    if (userInfo != null && isEligibleForBonus(userInfo.lastBonusDate)) {
                        bonusMessageTextView.setText("Claim your daily bonus of 50 points!");
                        claimBonusButton.setVisibility(View.VISIBLE);
                    } else {
                        bonusMessageTextView.setText("Get bonus next day.");
                        claimBonusButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
    private boolean isEligibleForBonus(String lastBonusDate) {
        LocalDate lastDate = LocalDate.parse(lastBonusDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return !LocalDate.now().isBefore(lastDate.plusDays(1));
    }
    private void claimDailyBonus(PointsUpdateListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            new UserPointsUpdater().updatePointsByEmail(user.getEmail(), 50, new PointsUpdateListener() {
                @Override
                public void onSuccess() {
                    // Update last bonus date to today
                    String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                    mDatabase.child("users").child(user.getUid()).child("lastBonusDate").setValue(today)
                            .addOnSuccessListener(aVoid -> {
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                listener.onFailure(e);
                            });
                }

                @Override
                public void onFailure(Exception exception) {
                    listener.onFailure(exception);
                }
            });
        } else {
            listener.onFailure(new Exception("User not authenticated"));
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // User is signed in
            userEmailTextView.setText(user.getEmail());
            mDatabase.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Assuming you have a User class that matches the structure in Firebase
                    User userInfo = dataSnapshot.getValue(User.class);
                    if (userInfo != null) {
                        userIdTextView.setText("ID: " + userInfo.id);
                        userPointsTextView.setText("Points: " + userInfo.points);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No user is signed in
            userEmailTextView.setText("Please login first");
            userIdTextView.setText(""); // Clear text
            userPointsTextView.setText(""); // Clear text
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
        onStart();
    }

    //test UserPointUpdater
    private void addOne(FirebaseUser user) {
        if (user != null) {
            new UserPointsUpdater().updatePointsByEmail(user.getEmail(), 1, new PointsUpdateListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> updateUI(mAuth.getCurrentUser()));
                }

                @Override
                public void onFailure(Exception exception) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void minusOne(FirebaseUser user) {
        if (user != null) {
            new UserPointsUpdater().updatePointsByEmail(user.getEmail(), -1, new PointsUpdateListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> updateUI(mAuth.getCurrentUser()));
                }

                @Override
                public void onFailure(Exception exception) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }


}