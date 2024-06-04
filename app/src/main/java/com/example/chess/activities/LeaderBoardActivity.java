package com.example.chess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.R;
import com.example.chess.firebase.User;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaderBoardActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private Button backButton;
    private ImageView firstPlaceProfileImage, secondPlaceProfileImage, thirdPlaceProfileImage;
    private TextView firstPlaceDetails, secondPlaceDetails, thirdPlaceDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        tableLayout = findViewById(R.id.gameHistoryTable);
        backButton = findViewById(R.id.backButton);

        firstPlaceProfileImage = findViewById(R.id.firstPlaceProfileImage);
        secondPlaceProfileImage = findViewById(R.id.secondPlaceProfileImage);
        thirdPlaceProfileImage = findViewById(R.id.thirdPlaceProfileImage);

        firstPlaceDetails = findViewById(R.id.firstPlaceDetails);
        secondPlaceDetails = findViewById(R.id.secondPlaceDetails);
        thirdPlaceDetails = findViewById(R.id.thirdPlaceDetails);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fetchAndDisplayUsersData();
    }


    private void addUserDataToTable(int place, String userName, String rating, String games, String profileImageUrl) {
        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        // Place
        TextView placeTextView = createTextView(String.valueOf(place));
        TableRow.LayoutParams placeParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1);
        placeTextView.setLayoutParams(placeParams);
        placeTextView.setGravity(Gravity.CENTER);
        tableRow.addView(placeTextView);

        // Profile Image
        ImageView profileImageView = new ImageView(this);
        TableRow.LayoutParams imageParams = new TableRow.LayoutParams(0, 80, 1);  // Use layout_weight to distribute width
        imageParams.setMargins(8, 8, 8, 8);
        profileImageView.setLayoutParams(imageParams);
        profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .into(profileImageView);
        }
        tableRow.addView(profileImageView);

        // Username
        TextView userNameTextView = createTextView(userName);
        TableRow.LayoutParams nameParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2.5f);  // Adjust weight
        userNameTextView.setLayoutParams(nameParams);
        userNameTextView.setGravity(Gravity.CENTER_VERTICAL);
        tableRow.addView(userNameTextView);

        // Rating
        TextView ratingTextView = createTextView(rating);
        TableRow.LayoutParams ratingParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1);
        ratingTextView.setLayoutParams(ratingParams);
        ratingTextView.setGravity(Gravity.CENTER_VERTICAL);
        tableRow.addView(ratingTextView);

        // Games
        TextView gamesTextView = createTextView(games);
        TableRow.LayoutParams gamesParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1);
        gamesTextView.setLayoutParams(gamesParams);
        gamesTextView.setGravity(Gravity.CENTER_VERTICAL);
        tableRow.addView(gamesTextView);

        // Define row parameters and margins
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 10);
        tableRow.setLayoutParams(rowParams);
        tableRow.setGravity(Gravity.CENTER_VERTICAL);

        tableLayout.addView(tableRow);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);

        // Apply the same styling attributes as title row
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
        textView.setBackgroundColor(getResources().getColor(R.color.faded_black));
        textView.setTextColor(getResources().getColor(R.color.dark_gray));

        return textView;
    }


    private void fetchAndDisplayUsersData() {
        FirebaseUtils firebaseUtils = new FirebaseUtils();
        firebaseUtils.fetchAllUsersDetails(new FirebaseUtils.AllUsersDetailsCallback() {
            @Override
            public void onAllUsersDetailsFetched(List<User> users) {
                int place = 1;  // Start from the 1st place
                // Sort users by rating in descending order
                Collections.sort(users, new Comparator<User>() {
                    @Override
                    public int compare(User user1, User user2) {
                        return user2.getRating() - user1.getRating();
                    }
                });

                if (users.size() > 0) {
                    User firstUser = users.get(0);
                    Glide.with(LeaderBoardActivity.this).load(firstUser.getProfileImageUrl()).into(firstPlaceProfileImage);
                    firstPlaceDetails.setText(firstUser.getUsername() + "\nRating: " + firstUser.getRating());
                    users.remove(0);
                    place++;
                }

                if (users.size() > 0) {
                    User secondUser = users.get(0);
                    Glide.with(LeaderBoardActivity.this).load(secondUser.getProfileImageUrl()).into(secondPlaceProfileImage);
                    secondPlaceDetails.setText(secondUser.getUsername() + "\nRating: " + secondUser.getRating());
                    users.remove(0);
                    place++;
                }

                if (users.size() > 0) {
                    User thirdUser = users.get(0);
                    Glide.with(LeaderBoardActivity.this).load(thirdUser.getProfileImageUrl()).into(thirdPlaceProfileImage);
                    thirdPlaceDetails.setText(thirdUser.getUsername() + "\nRating: " + thirdUser.getRating());
                    users.remove(0);
                    place++;
                }

                for (User user : users) {
                    String userName = user.getUsername();
                    String rating = String.valueOf(user.getRating());
                    String games = String.valueOf(user.getGames());
                    String profileImageUrl = user.getProfileImageUrl();

                    addUserDataToTable(place, userName, rating, games, profileImageUrl);
                    place++;  // Increment the place for the next user
                }
            }
        });
    }


}
