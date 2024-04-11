package edu.northeastern.numad24sp_team33_final;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity;

public class MainActivity extends AppCompatActivity {
    private TextView userEmailTextView, userIdTextView, userPointsTextView, bonusMessageTextView;
    private Button logoutButton, reg, login, testPlusOneBtn, testMinusOneBtn, claimBonusButton, leaderboardButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String userUID;
    private LinearLayout dynamicButtonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        userEmailTextView = findViewById(R.id.userEmailTextView);
        userIdTextView = findViewById(R.id.userIdTextView);
        userPointsTextView = findViewById(R.id.userPointsTextView);

        userUID = mAuth.getCurrentUser().getUid();

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

        dynamicButtonContainer = findViewById(R.id.dynamicButtonContainer);


        fetchLatestThreeBetsForUser(userUID, betNames -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
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


    private void addDynamicButtons(List<String> companyNames) {

        //no companies
        if (companyNames.size() == 0) {
            // Create a TextView to show "no recent companies"
            TextView noCompaniesView = new TextView(this);
            noCompaniesView.setText("No recent companies");
            // Optionally, style your TextView here
            noCompaniesView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            dynamicButtonContainer.addView(noCompaniesView);
            return; // Return early as there's nothing more to do
        }

        for (String companyName : companyNames) {
            Button button = new Button(this);
            button.setText(companyName);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, AssetStatusActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(AssetStatusActivity.TICKER_SYMBOL_KEY, companyName);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });

            // Set layout parameters for the button
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 10, 0); // right margin
            button.setLayoutParams(params);

            dynamicButtonContainer.addView(button);
        }
    }

    public interface OnBetNamesFetched {
        void onBetNamesFetched(List<String> betNames);
    }

    private void fetchLatestThreeBetsForUser(final String userId, final OnBetNamesFetched callback) {
        List<String> betNames = new ArrayList<>();
        final DatabaseReference betsRef = mDatabase.child("bets");
        betsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This list will hold the strings representing each bet found for the user
                List<String> userBets = new ArrayList<>();

                // Iterate through each asset
                for (DataSnapshot assetSnapshot : dataSnapshot.getChildren()) {
                    String assetId = assetSnapshot.getKey();

                    // Iterate through each date
                    for (DataSnapshot dateSnapshot : assetSnapshot.getChildren()) {
                        String date = dateSnapshot.getKey();

                        // Iterate through each bet under the date
                        for (DataSnapshot betSnapshot : dateSnapshot.getChildren()) {
                            DataSnapshot userIdSnapshot = betSnapshot.child("userId");
                            if (userIdSnapshot.exists() && userId.equals(userIdSnapshot.getValue(String.class))) {
                                // Construct a string representation for the bet
                                String betRepresentation = assetId + " on " + date;
                                userBets.add(betRepresentation);
                            }
                        }
                    }
                }

                // Sort the list of bets by date
                Collections.sort(userBets, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        // Extract the date part of the string and compare. This assumes the date is at the end of the string.
                        return extractDate(o1).compareTo(extractDate(o2));
                    }

                    private LocalDate extractDate(String betRepresentation) {
                        // Assuming the format is "ASSETID on YYYY-MM-DD"
                        String[] parts = betRepresentation.split(" on ");
                        return LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("MM-dd-yyyy"));
                    }
                });

                // Keep only the latest three bets, if there are more than three
                if (userBets.size() > 3) {
                    userBets = userBets.subList(userBets.size() - 3, userBets.size());
                }

                // Reverse to have the most recent first
                Collections.reverse(userBets);

                // Update the betNames with the latest bets, ONLY add first part, remove identical bet names
                for (String bet : userBets) {
                    String[] parts = bet.split(" on ");
                    String betName = parts[0];
                    if (!betNames.contains(betName)) {
                        betNames.add(betName);
                    }
                }
                if (callback != null) {
                    callback.onBetNamesFetched(betNames);
                }

                List<String> recentCompanies = betNames;
                addDynamicButtons(recentCompanies);



            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("DatabaseError", "fetchLatestThreeBetsForUser:onCancelled", databaseError.toException());
            }
        });
    }


}