package com.example.chess.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.R;
import com.example.chess.firebase.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;


import java.util.HashMap;
import java.util.Map;

public class profileActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView ratingTextView;
    private TextView pickImageText;
    private EditText imageUrlEditText;

    private Button editProfileButton;
    private Button signOutButton;
    private Button fetchFromUrlButton;
    private Button captureNewImageButton;

    private ImageView profilePicture;
    private String username;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private FirebaseUser currentUser;
    private FirebaseUtils firebaseUtils;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind views from XML
        profilePicture = findViewById(R.id.profilePicture);
        pickImageText = findViewById(R.id.pickImageText);
        imageUrlEditText = findViewById(R.id.imageUrlEditText);
        signOutButton = findViewById(R.id.signOutButton);
        usernameTextView = findViewById(R.id.username);
        emailTextView = findViewById(R.id.email);
        ratingTextView = findViewById(R.id.rating);
        editProfileButton = findViewById(R.id.editProfileButton);
        fetchFromUrlButton = findViewById(R.id.fetchFromUrlButton);
        MaterialButton captureImageButton = findViewById(R.id.captureImageButton);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUtils = new FirebaseUtils();
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String currentUsername = sharedPreferences.getString("loggedInUser", null);
        if (currentUsername != null){
            username = username;
        } else{
            firebaseUtils.fetchUsername(currentUser, currUsername -> {
                if (currUsername != null) {
                    username = currUsername;
                }
            });
        }


        profilePicture.setVisibility(View.GONE);
        pickImageText.setVisibility(View.VISIBLE);

        fetchAndDisplayUsername();
        fetchAndDisplayUserData();
        loadProfileImage();

        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        fetchFromUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String imageUrl = imageUrlEditText.getText().toString().trim();
                if (!imageUrl.isEmpty()) {
                    // Display the image using Glide
                    Glide.with(profileActivity.this)
                            .load(imageUrl)
                            .into(profilePicture);

                    // Update the Firebase Database with the URL directly
                    Map<String, Object> newCredentials = new HashMap<>();
                    newCredentials.put("profileImageUrl", imageUrl);

                    firebaseUtils.updateCredentials(currentUser, newCredentials, new FirebaseUtils.UpdateCredentialsCallback() {
                        @Override
                        public void onUpdateCredentials(boolean success) {
                            if (success) {
                                Toast.makeText(profileActivity.this, "Image URL saved successfully!", Toast.LENGTH_SHORT).show();

                                // Add a delay before reloading the image
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadProfileImage(); // Call loadProfileImage after a delay
                                        imageUrlEditText.setText("");
                                    }
                                }, 3000);
                            } else {
                                Toast.makeText(profileActivity.this, "Error saving image URL to Firestore!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(profileActivity.this, "Please provide a valid image URL!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle profile edit
                handleEditProfile();
            }
        });
    }
    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void fetchAndDisplayUsername() {
        firebaseUtils.fetchUsername(currentUser, new FirebaseUtils.UsernameCallback() {
            @Override
            public void onUsernameFetched(String fetchedUsername) {
                usernameTextView.setText(fetchedUsername);
            }
        });
    }
    private void fetchAndDisplayUserData() {
        firebaseUtils.fetchUserDetails(currentUser, new FirebaseUtils.UserDetailsCallback() {
            @Override
            public void onUserDetailsFetched(User user) {
                usernameTextView.setText("Name: " + user.getUsername());
                emailTextView.setText("Email: " + user.getEmail());
                ratingTextView.setText("Rating: " + user.getRating());
                Log.d("ProfileActivity", "Fetched username: " +  user.getEmail());
                Log.d("ProfileActivity", "Fetched username: " + user.getRating());

            }
        });
    }
    private void loadProfileImage() {
        firebaseUtils.fetchUserDetails(currentUser, new FirebaseUtils.UserDetailsCallback() {
            @Override
            public void onUserDetailsFetched(User user) {
                String profileImageUrl = user.getProfileImageUrl();
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    // Use Glide to load the image
                    Glide.with(profileActivity.this)
                            .load(profileImageUrl)
                            .into(profilePicture);

                    profilePicture.setVisibility(View.VISIBLE);
                    pickImageText.setVisibility(View.GONE);
                } else {
                    profilePicture.setVisibility(View.GONE);
                    pickImageText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void handleEditProfile() {
        // TODO: Implement edit profile functionalityd
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            profilePicture.setImageBitmap(imageBitmap);

            profilePicture.setVisibility(View.VISIBLE);
            pickImageText.setVisibility(View.GONE);

            // Upload image to Firebase Storage
            firebaseUtils.uploadImageToFirebaseStorage(imageBitmap, new FirebaseUtils.OnImageUploadCallback() {
                @Override
                public void onImageUpload(String imageUrl) {
                    if (imageUrl != null) {
                        // Save the imageUrl to Firestore
                        Map<String, Object> newCredentials = new HashMap<>();
                        newCredentials.put("profileImageUrl", imageUrl);

                        firebaseUtils.updateCredentials(currentUser, newCredentials, new FirebaseUtils.UpdateCredentialsCallback() {
                            @Override
                            public void onUpdateCredentials(boolean success) {
                                if (success) {
                                    Toast.makeText(profileActivity.this, "Image uploaded and saved successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(profileActivity.this, "Error saving image URL to Firestore!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(profileActivity.this, "Error uploading image!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    private void signOut(){
//        Log.d("ProfileActivity", "Fetched username: " + username);
//        firebaseUtils.removeFromActivePlayers(username);
        FirebaseAuth.getInstance().signOut();

        SharedPreferences settingsSharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsSharedPreferences.edit();
        editor.clear();
        editor.apply();
        editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(profileActivity.this, MenuActivity.class));
            }
        }, 2000);
    }


}
