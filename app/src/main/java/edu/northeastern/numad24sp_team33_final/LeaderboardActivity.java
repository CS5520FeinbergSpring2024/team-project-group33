package edu.northeastern.numad24sp_team33_final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> entries = new ArrayList<>();
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recyclerView = findViewById(R.id.leaderboard_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));



        adapter = new LeaderboardAdapter(entries);
        recyclerView.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        mDatabase.child("users").orderByChild("points").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                entries.clear(); // Clear existing entries
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String name = userSnapshot.child("email").getValue(String.class); // Using email as the name for demonstration
                    Integer points = userSnapshot.child("points").getValue(Integer.class);

                    if (name != null && points != null) {
                        entries.add(new LeaderboardEntry(name, points));
                    }
                }
                Collections.reverse(entries); // Firebase returns in ascending order, reverse for descending
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("LeaderboardActivity", "loadLeaderboard:onCancelled", databaseError.toException());
            }
        });
    }


}
