package edu.northeastern.numad24sp_team33_final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegisterActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    EditText emailEditText, passwordEditText, userIdEditText;
    Button registerButton;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        userIdEditText = findViewById(R.id.userIdEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String userID = userIdEditText.getText().toString().trim();

            // Basic validation
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterActivity.this, "Please enter an email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(userID)) {
                Toast.makeText(RegisterActivity.this, "Please enter a user ID.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the user ID is already taken
            mDatabase.child("users").orderByChild("id").equalTo(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // User ID is unique, proceed with registration
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(RegisterActivity.this, task -> {
                                    if (task.isSuccessful()) {
                                        // Registration success
                                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                        String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                                        User newUser = new User(email, userID, 1000, date);
                                        // Add user to the database
                                        mDatabase.child("users").child(firebaseUser.getUid()).setValue(newUser)
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                                        // Proceed with navigation or UI update
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Failed to register user details.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        // Registration failed
                                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // User ID already exists
                        Toast.makeText(RegisterActivity.this, "User ID already exists.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(RegisterActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}