package com.example.chess.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chess.R;

import com.example.chess.firebase.FirebaseUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText emailEditText;
    private TextView textViewLogin;
    private Button signUpButton;
    private Button homeButton;

    private Spinner playerLevel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUtils firebaseUtils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sigup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUtils = new FirebaseUtils();
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        emailEditText = findViewById(R.id.emailEditText);
        textViewLogin = findViewById(R.id.textViewLogin);
        signUpButton = findViewById(R.id.signUpButton);
        homeButton = findViewById(R.id.homeButton);
        playerLevel = findViewById(R.id.playerLevel);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (!username.isEmpty() && !password.isEmpty() && !email.isEmpty()) {
                    createUser(email, password, username);
                }
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SignUpActivity.this, "Home button clicked!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, MenuActivity.class));
            }
        });

    }

    private int getPlayerLevel() {
        String selectedLevel = playerLevel.getSelectedItem().toString();

        switch (selectedLevel) {
            case "Beginner":
                return 600;
            case "Intermediate":
                return 1100;
            case "Advanced":
                return 1600;
            case "Expert":
                return 2000;
            default:
                return 600;
        }
    }

    private void createUser(String email, String password, final String username) {
        // First, check if the username already exists
        firebaseUtils.checkUsernameExists(username, exists -> {
            if (exists) {
                // If the username exists, inform the user and do not proceed with registration
                Toast.makeText(SignUpActivity.this, "Username already taken. Please choose another.", Toast.LENGTH_SHORT).show();
            } else {
                // If the username does not exist, proceed with the registration
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                // Create user details map to save in Firestore
                                Map<String, Object> user = new HashMap<>();
                                user.put("email", email);
                                user.put("username", username);
                                user.put("rating", getPlayerLevel());
                                user.put("games", 0);
                                user.put("gameHistory", new ArrayList<>()); // Initialize an empty game history list

                                // Save user details to Firestore
                                db.collection("users").document(firebaseUser.getUid()).set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(SignUpActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SignUpActivity.this, "Failed to save user details.", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
