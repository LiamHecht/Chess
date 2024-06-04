package com.example.chess.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;

import com.example.chess.AppLifecycleService;
import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class MenuActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextView signIn;
    private ImageView settingsButton;
    private Button local1v1Button;
    private Button online1v1Button;
    private Button againstBot1v1Button;
    private TextView initialTimeView;
    private TextView bonusTimeView;
    private int whiteSideSquareColor = Color.parseColor("#FFFFFF");
    private int blackSideSquareColor = Color.parseColor("#E0E0E0");
    private int availableMovesColor = Color.parseColor("#ADD8E6");
    private int selectedPieceColor = Color.parseColor("#FF7F7F");

//    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FirebaseUtils firebaseUtils;
    private TableLayout tableLayout;

    private String currentUsername = "";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private FirebaseDatabase database;
    private DatabaseReference matchedGamesRef;
    private DatabaseReference waitingPlayersRef;

    private ValueEventListener gameRequestListener;
    private SharedPreferences sharedPreferences;
    private String roomId;

    private boolean isSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);
        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));

        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setting up the toggle button for the drawer
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        signIn = findViewById(R.id.signIn);
        settingsButton = findViewById(R.id.settingsButton);

        local1v1Button = findViewById(R.id.local1v1Button);
        online1v1Button = findViewById(R.id.online1v1Button);
        againstBot1v1Button = findViewById(R.id.againstBot1v1Button);

        tableLayout = findViewById(R.id.gameHistoryTable);


        FirebaseApp.initializeApp(this);
        database = FirebaseDatabase.getInstance("https://chess-6d839-default-rtdb.europe-west1.firebasedatabase.app");
        matchedGamesRef = database.getReference("game_rooms/matched_games");
        waitingPlayersRef = database.getReference("game_rooms/waiting_players");
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("loggedInUser", null);

        firebaseUtils = new FirebaseUtils();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent serviceIntent = new Intent(this, AppLifecycleService.class);
        startService(serviceIntent);

        if (currentUsername != null){
            signIn.setText(currentUsername);
            listenForGameRequests();
//            firebaseUtils.addToActivePlayers(currentUsername);
        }

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUser != null) {
                    startActivity(new Intent(MenuActivity.this, profileActivity.class));
                } else {
                    startActivity(new Intent(MenuActivity.this, LoginActivity.class));
                }
            }
        });


        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MenuActivity.this, SettingsActivity.class);
                startActivityForResult(settingsIntent, 1);
            }
        });


        local1v1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSearch){
                    showToast("cant start game while searching");
                    return;
                }
                startLocalGame();

            }
        });

        online1v1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog popupDialog = new Dialog(MenuActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
                popupDialog.setContentView(R.layout.activity_onlinemenu);

                // Handle the button clicks
                Spinner sideSpinner = popupDialog.findViewById(R.id.spinner1);
                Spinner modeSpinner = popupDialog.findViewById(R.id.spinner2);
                EditText timeEditText = popupDialog.findViewById(R.id.editText1);
                EditText incrementEditText = popupDialog.findViewById(R.id.editText2);
                Button asKToPlayButton = popupDialog.findViewById(R.id.btnAskToPlay);
                Button startWithRandomGameButton = popupDialog.findViewById(R.id.startGameButton);
                EditText opponentEditText = popupDialog.findViewById(R.id.opponentEditText);


                startWithRandomGameButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentUser == null) {
                            Toast.makeText(MenuActivity.this, "Please log in to play online!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (isSearch){
                            Toast.makeText(MenuActivity.this, "Can't create more than 1 game room", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String selectedSide = sideSpinner.getSelectedItem().toString();
                        String mode = modeSpinner.getSelectedItem().toString();

                        // Check if the timeEditText and incrementEditText are empty
                        if(timeEditText.getText().toString().isEmpty() || incrementEditText.getText().toString().isEmpty()) {
                            Toast.makeText(MenuActivity.this, "Please enter both time and increment values!", Toast.LENGTH_LONG).show();
                            return; // Return early so the rest of the method doesn't execute
                        }

                        try {
                            long time = Long.parseLong(timeEditText.getText().toString());
                            long increment = Long.parseLong(incrementEditText.getText().toString());

                            // Validate time and increment
                            if (time <= 120 && increment <= 15) {
                                isSearch = true;
                                addCurrentUsertoWaitingList(selectedSide, mode, time, increment);
                                popupDialog.dismiss();
                            } else {
                                Toast.makeText(MenuActivity.this, "Time must be no longer than 120 seconds and increment no bigger than 15 seconds!", Toast.LENGTH_LONG).show();
                            }
                        } catch(NumberFormatException e) {
                            Toast.makeText(MenuActivity.this, "Invalid time or increment value entered!", Toast.LENGTH_LONG).show();
                        }
                    }
                });




                asKToPlayButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentUser == null) {
                            Toast.makeText(MenuActivity.this, "Please log in to play online!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String selectedSide = sideSpinner.getSelectedItem().toString();
                        String mode = modeSpinner.getSelectedItem().toString();
                        String opponent = opponentEditText.getText().toString();

                        // Check if the entered opponent's username exists
                        firebaseUtils.fetchUidForUsername(opponent, uid -> {
                            if (uid != null) {
                                // Check if the user is online
                                firebaseUtils.fetchActiveUsers(activeUserIds -> {
                                    if (activeUserIds.contains(opponent)) {
                                        try {
                                            // Ensure fields aren't empty before parsing
                                            String timeStr = timeEditText.getText().toString();
                                            String incrementStr = incrementEditText.getText().toString();
                                            if (!timeStr.isEmpty() && !incrementStr.isEmpty()) {
                                                long time = Long.parseLong(timeStr);
                                                long increment = Long.parseLong(incrementStr);

                                                // Validate time and increment
                                                if (time <= 120 && increment <= 15) {
                                                    // Invert the side for the opponent
                                                    String opponentSide = "White".equalsIgnoreCase(selectedSide) ? "Black" : "White";

                                                    askToPlay(opponent, opponentSide, mode, time, increment);
                                                    popupDialog.dismiss();
                                                } else {
                                                    Toast.makeText(MenuActivity.this, "Time must be no longer than 120 seconds and increment no bigger than 15 seconds!", Toast.LENGTH_LONG).show();
                                                }
                                            } else {
                                                Toast.makeText(MenuActivity.this, "Please enter both time and increment values!", Toast.LENGTH_LONG).show();
                                            }
                                        } catch (NumberFormatException e) {
                                            Toast.makeText(MenuActivity.this, "Invalid time or increment value entered!", Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        // User is not online
                                        Toast.makeText(MenuActivity.this, opponent + " is not online!", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                // User does not exist
                                Toast.makeText(MenuActivity.this, "Entered username does not exist!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                popupDialog.show();  // Show the popup dialog

            }
        });

        againstBot1v1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSearch){
                    showToast("cant start game while searching");
                    return;
                }
                startGameAgainstBot();
            }
        });
        displayWaitingPlayersInTable();
        displayGameRoomsInTable();
    }

        //----------------------------------------------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        isSearch = false;
    }

    private void listenForGameRequests() {
            DatabaseReference gameRequestsRef = database.getReference("game_requests").child(currentUsername);

            gameRequestListener = gameRequestsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> gameRequest = (Map<String, Object>) dataSnapshot.getValue();
                        String challenger = (String) gameRequest.get("challenger");
                        String status = (String) gameRequest.get("status");

                        if ("pending".equals(status)) {
                            showGameRequestNotification(challenger);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MenuActivity", "Error listening for game requests", databaseError.toException());
                }
        });
    }
    // Function to Listen for Responses to Sent Game Requests
    private void listenForGameResponses() {
        if (currentUsername == null) return;

        DatabaseReference gameResponseRef = database.getReference("notifications").child(currentUsername);

        gameResponseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot notificationSnap : dataSnapshot.getChildren()) {
                        Map<String, Object> notification = (Map<String, Object>) notificationSnap.getValue();
                        String type = (String) notification.get("type");
                        String opponent = (String) notification.get("from");
                        String status = (String) notification.get("status");
                        DatabaseReference gameRequestsRef = database.getReference("game_requests").child(opponent);

                        if ("game_response".equals(type)) {
                            if ("accepted".equals(status)) {
                                showToast(opponent + " accepted your challenge!");
                                gameRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            Map<String, Object> gameDetails = (Map<String, Object>) dataSnapshot.getValue();
                                            String side = (String) gameDetails.get("side");
                                            boolean isWhite = "White".equalsIgnoreCase(side);
                                            isWhite = !isWhite;  // Invert the side

                                            long timeValueInMinutes = (long) gameDetails.get("time");
                                            long incrementValue = (long) gameDetails.get("increment");

                                            // Convert the game request to a game room
                                            DatabaseReference gameRoomsRef = database.getReference("game_rooms");
                                            String roomId = currentUsername;
                                            //Liam
                                            Map<String, Object> gameRoomDetails = new HashMap<>();
                                            gameRoomDetails.put("player1", currentUsername);
                                            gameRoomDetails.put("player2", opponent);
                                            gameRoomDetails.put("status", "active");
                                            gameRoomDetails.put("time", timeValueInMinutes);
                                            gameRoomDetails.put("increment", incrementValue);

                                            // Create the game room
                                            boolean finalIsWhite = isWhite;
                                            gameRoomsRef.child(roomId).setValue(gameRoomDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    // Once the game room is created successfully, remove the game request
                                                    gameRequestsRef.removeValue();
                                                    // Call the startOnlineGame function
                                                    startOnlineGame(finalIsWhite, currentUsername, timeValueInMinutes, incrementValue,opponent, "creator");
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("MenuActivity", "Error fetching game details", databaseError.toException());
                                    }
                                });

                            } else if ("declined".equals(status)) {
                                showToast(opponent + " declined your challenge.");
                                gameRequestsRef.removeValue();
                            }
                            notificationSnap.getRef().removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MenuActivity", "Error listening for game responses", databaseError.toException());
            }
        });
    }


    // Function to Show a Notification for a Received Game Request
    private void showGameRequestNotification(String challenger) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(challenger + " has challenged you to a game!");
        builder.setMessage("Do you accept the challenge?");

        builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                respondToGameRequest(challenger, true);
            }
        });

        builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                respondToGameRequest(challenger, false);
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    // Function to Respond to a Game Request
    private void respondToGameRequest(String challenger, boolean accepted) {
        DatabaseReference gameRequestsRef = database.getReference("game_requests").child(currentUsername);

        if (accepted) {
            gameRequestsRef.child("status").setValue("accepted");

            // Fetch game details and start the game
            gameRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                //Liam
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> gameDetails = (Map<String, Object>) dataSnapshot.getValue();
                        String side = (String) gameDetails.get("side");
                        boolean isWhite = "White".equalsIgnoreCase(side);
                        long timeValueInMinutes = (long) gameDetails.get("time");
                        long incrementValue = (long) gameDetails.get("increment");
                        if (isSearch){
                            removeFromWaitingList(currentUsername);
                            isSearch = false;
                        }
                        // Call the startOnlineGame function
                        startOnlineGame(isWhite, currentUsername, timeValueInMinutes, incrementValue,challenger,"participte");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MenuActivity", "Error fetching game details", databaseError.toException());
                }
            });

        } else {
            gameRequestsRef.child("status").setValue("declined");

        }

        // Notify the challenger about the response
        DatabaseReference challengerNotificationRef = database.getReference("notifications").child(challenger);
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", "game_response");
        notificationData.put("from", currentUsername);
        notificationData.put("status", accepted ? "accepted" : "declined");
        challengerNotificationRef.push().setValue(notificationData);
    }
    private void askToPlay(String targetUsername, String side, String mode, long time, long increment) {
        FirebaseApp.initializeApp(this);
        DatabaseReference gameRequestsRef = database.getReference("game_requests");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        firebaseUtils.fetchUsername(currentUser, username -> {
            if (username != null) {
                Map<String, Object> gameRequest = new HashMap<>();
                gameRequest.put("challenger", username);
                gameRequest.put("status", "pending");
                gameRequest.put("side", side);
                gameRequest.put("mode", mode);
                gameRequest.put("time", time);
                gameRequest.put("increment", increment);

                gameRequestsRef.child(targetUsername).setValue(gameRequest);
                listenForGameResponses();
            }
        });
    }


    private void addCurrentUsertoWaitingList(String side, String mode, long time, long increment) {
//        DatabaseReference waitingPlayersRef = database.getReference("game_rooms/waiting_players");

        Map<String, Object> waitingPlayerDetails = new HashMap<>();
        waitingPlayerDetails.put("side", side);
        waitingPlayerDetails.put("mode", mode);
        waitingPlayerDetails.put("time", time);
        waitingPlayerDetails.put("increment", increment);
        waitingPlayersRef.child(currentUsername).setValue(waitingPlayerDetails)
                .addOnSuccessListener(aVoid -> {
                    // Pass the chosen side to the listening method
                    listenForOtherPlayerToJoin(side);
                });

        Toast.makeText(MenuActivity.this, "Waiting for a random opponent...", Toast.LENGTH_LONG).show();
    }
    private void listenForOtherPlayerToJoin(String chosenSide) {
        DatabaseReference matchedGamesRef = database.getReference("game_rooms/matched_games");

        Query playerJoinedQuery = matchedGamesRef.orderByChild("player1").equalTo(currentUsername);

        playerJoinedQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {

                        String timeStr = gameSnapshot.child("time").getValue(String.class);
                        String incrementStr = gameSnapshot.child("increment").getValue(String.class);
                        try {
                            Long gameTime = Long.parseLong(timeStr);
                            Long gameIncrement = Long.parseLong(incrementStr);

                            if (gameTime != null && gameIncrement != null) {
//                                showToast("A player has joined!");
                                String opponent = gameSnapshot.child("player2").getValue(String.class);
//                                showToast(opponent);
                                boolean isWhite = "white".equalsIgnoreCase(chosenSide);

                                startOnlineGame(isWhite, currentUsername, gameTime, gameIncrement,opponent,"creator");

                                playerJoinedQuery.removeEventListener(this);
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Log.e("MenuActivity", "Failed to parse time or increment to long", e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MenuActivity", "Error listening for other player", databaseError.toException());
            }
        });
    }
    private TextView createClickableTextView(String text, String opponentUsername, String time, String increment) {
        TextView textView = createTextView(text);
        textView.setTextColor(getResources().getColor(R.color.light_blue));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text.equals("Cancel")) {
                    removeFromWaitingList(currentUsername);
                    isSearch = false;
                } else if (text.equals("Join")) {
                    if (currentUser == null) {
                        Toast.makeText(MenuActivity.this, "Please sign in to join a game!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentUsername.equals(opponentUsername)) {
                        Toast.makeText(MenuActivity.this, "You can't join your own room!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isSearch){
                        waitingPlayersRef.child(currentUsername).removeValue();
                    }

                    waitingPlayersRef.child(opponentUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String creatorSide = dataSnapshot.child("side").getValue(String.class);
                                boolean isCreatorWhite = "white".equalsIgnoreCase(creatorSide);
                                boolean isOpponentWhite = !isCreatorWhite;

                                // Remove the creator from the waiting list since the game is about to start
                                waitingPlayersRef.child(opponentUsername).removeValue().addOnSuccessListener(aVoid -> {
                                    roomId = opponentUsername;
                                    DatabaseReference specificMatchedGameRef = matchedGamesRef.child(roomId);
                                    Map<String, Object> gameDetails = new HashMap<>();
                                    gameDetails.put("player1", opponentUsername);
                                    gameDetails.put("player2", currentUsername);
                                    gameDetails.put("isCreatorWhite", isCreatorWhite);
                                    gameDetails.put("time", time);
                                    gameDetails.put("increment", increment);
                                    specificMatchedGameRef.setValue(gameDetails);
                                    Log.e("test", "part");
                                    startOnlineGame(isOpponentWhite, currentUsername, Long.parseLong(time), Long.parseLong(increment),opponentUsername, "participate");
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MenuActivity", "Error fetching side of the creator", databaseError.toException());
                        }
                    });
                } else if (text.equals("spectate")) {
                    // Fetch details about the game from matched_games
                    DatabaseReference specificMatchedGameRef = matchedGamesRef.child(opponentUsername);
                    specificMatchedGameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Retrieve game details
                                String player1 = dataSnapshot.child("player1").getValue(String.class);
                                String player2 = dataSnapshot.child("player2").getValue(String.class);
                                boolean isCreatorWhite = dataSnapshot.child("isCreatorWhite").getValue(boolean.class);
                                String time = dataSnapshot.child("time").getValue(String.class);
                                String increment = dataSnapshot.child("increment").getValue(String.class);

                                startGameAsSpectator(player1, player2, isCreatorWhite);
                            } else {
                                // Handle the case where the game details are not found
                                Toast.makeText(MenuActivity.this, "Game details not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MenuActivity", "Error fetching game details", databaseError.toException());
                        }
                    });
                }

            }
        });
        return textView;
    }
    private void removeFromWaitingList(String username) {
        waitingPlayersRef.child(username).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(MenuActivity.this, "Game request cancelled!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("MenuActivity", "Error removing from waiting list", e);
            Toast.makeText(MenuActivity.this, "Error cancelling game request. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private void displayWaitingPlayersInTable() {
        tableLayout.removeAllViews();  // Clear the table first
        displayTableTitles();  // Display the column titles

        waitingPlayersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tableLayout.removeAllViews();  // Clear previous rows every time data changes
                displayTableTitles();  // Re display the column titles

                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    String player1 = playerSnapshot.getKey();
                    String player2 = "waiting";
                    String time = playerSnapshot.child("time").getValue(Long.class).toString();
                    String increment = playerSnapshot.child("increment").getValue(Long.class).toString();

                    addWaitingPlayerToTable(player1, player2, time, increment,"waiting");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching waiting players", databaseError.toException());
            }
        });
    }
    private void displayGameRoomsInTable() {
        tableLayout.removeAllViews();  // Clear the table first
        displayTableTitles();  // Display the column titles

//        DatabaseReference waitingPlayersRef = database.getReference("game_rooms/matched_games");
        matchedGamesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tableLayout.removeAllViews();  // Clear previous rows every time data changes
                displayTableTitles();  // Re display the column titles

                for (DataSnapshot playerSnapshot : dataSnapshot.getChildren()) {
                    String player1 = playerSnapshot.getKey();
                    String player2 = playerSnapshot.child("player2").getValue(String.class);
                    String time = "";
                    String increment = "";

                    // Retrieve time and increment as strings first
                    Object timeObj = playerSnapshot.child("time").getValue();
                    Object incrementObj = playerSnapshot.child("increment").getValue();

                    // Convert time and increment to strings if not null
                    if (timeObj != null && incrementObj != null) {
                        time = timeObj.toString();
                        increment = incrementObj.toString();
                    }

                    // Add error handling for parsing time and increment to long
                    try {
                        long timeValue = Long.parseLong(time);
                        long incrementValue = Long.parseLong(increment);
                        addWaitingPlayerToTable(player1, player2, Long.toString(timeValue), Long.toString(incrementValue), "spectate");
                    } catch (NumberFormatException e) {
                        Log.e("ParseError", "Failed to parse time or increment as long: " + e.getMessage());
                        // Handle the error appropriately, such as displaying a message to the user or skipping this entry
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching waiting players", databaseError.toException());
            }
        });
    }

    private void addWaitingPlayerToTable(String player1, String player2, String time, String increment,String mode) {
        TableRow tableRow = new TableRow(this);

        TextView player1TextView = createTextView(player1);
        TextView player2TextView = createTextView(player2);
        TextView timeTextView = createTextView(time);
        TextView incrementTextView = createTextView(increment);

        // Check if current user is the creator of the room
        String actionText;
        if (mode.equals("waiting")){
            actionText = player1.equals(currentUsername) ? "Cancel" : "Join";
        } else {
            actionText = "spectate";
        }
        TextView actionTextView = createClickableTextView(actionText, player1, time, increment);

        tableRow.addView(player1TextView);
        tableRow.addView(player2TextView);
        tableRow.addView(timeTextView);
        tableRow.addView(incrementTextView);
        tableRow.addView(actionTextView);

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

    private void displayTableTitles() {
        TableRow tableRow = new TableRow(this);

        TextView player1Title = createTextView("Player 1");
        TextView player2Title = createTextView("Player 2");
        TextView timeTitle = createTextView("Time");
        TextView incrementTitle = createTextView("Increment");
        TextView actionTitle = createTextView("Action");

        tableRow.addView(player1Title);
        tableRow.addView(player2Title);
        tableRow.addView(timeTitle);
        tableRow.addView(incrementTitle);
        tableRow.addView(actionTitle);

        tableLayout.addView(tableRow);
    }

    private void startOnlineGame(boolean isWhite, String username, long timeValueInMinutes, long incrementValue,String opponentName, String user) {
        // Convert the provided values
        long timeValueInMillis = timeValueInMinutes * 60 * 1000;
        long incrementValueInMillis = incrementValue * 1000;

        Intent intent = new Intent(MenuActivity.this, OnlineGameActivity.class);
        intent.putExtra("side", isWhite);
        intent.putExtra("username", username);
        intent.putExtra("opponentUsername", opponentName);
        intent.putExtra("availableMovesColor", availableMovesColor);
        intent.putExtra("selectedPieceColor", selectedPieceColor);
        intent.putExtra("timeValueInMillis", timeValueInMillis);
        intent.putExtra("incrementValueInMillis", incrementValueInMillis);
        intent.putExtra("incrementValueInMillis", incrementValueInMillis);
        intent.putExtra("roomID", user);

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameRequestListener != null && currentUsername != null) {
            database.getReference("game_requests").child(currentUsername).removeEventListener(gameRequestListener);
        }
        waitingPlayersRef.child(currentUsername).removeValue();

    }

    // Function to Start a Game in Spectator Mode
    private void startGameAsSpectator(String player1, String player2, boolean isCreatorWhite) {
        Intent intent = new Intent(MenuActivity.this, SpectatorActivity.class);

        intent.putExtra("isSpectator", true);
        intent.putExtra("username", currentUsername);
        intent.putExtra("isCreatorWhite", isCreatorWhite);
        intent.putExtra("player1", player1);
        intent.putExtra("player2", player2);
        startActivity(intent);
    }

    // Function to Start a Local Game
    private void startLocalGame(){
        final Dialog popupDialog = new Dialog(MenuActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth); // Use a theme without borders or padding
        popupDialog.setContentView(R.layout.activity_startgame);

        // Make sure the dialog takes up the full width of the screen
        popupDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        // Make the dialog's window transparent so your layout's background is visible
        popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Define the spinners and buttons from the layout
        Spinner spinner1 = popupDialog.findViewById(R.id.spinner1);
        Button startGame = popupDialog.findViewById(R.id.startGameButton);
        final EditText editTextTime = popupDialog.findViewById(R.id.editText1);
        final EditText editTextIncrement = popupDialog.findViewById(R.id.editText2);

        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedSide = spinner1.getSelectedItem().toString();
                String timeValueInMinutes = editTextTime.getText().toString();
                String incrementValue = editTextIncrement.getText().toString();

                // Check if the timeEditText and incrementEditText are empty
                if(timeValueInMinutes.isEmpty() || incrementValue.isEmpty()) {
                    Toast.makeText(MenuActivity.this, "Please enter both time and increment values!", Toast.LENGTH_LONG).show();
                    return; // Return early so the rest of the method doesn't execute
                }

                long timeValueInMillis = 0;
                long incrementValueInMillis = 0;
                try {
                    long timeValueInMinutesLong = Long.parseLong(timeValueInMinutes);
                    if (timeValueInMinutesLong > 120) {
                        Toast.makeText(MenuActivity.this, "Time must be no longer than 120 minutes!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    timeValueInMillis = timeValueInMinutesLong * 60 * 1000;
                    incrementValueInMillis = Long.parseLong(incrementValue) * 1000;

                    // Validate increment
                    if (incrementValueInMillis > 15 * 1000) {
                        Toast.makeText(MenuActivity.this, "Increment must not be bigger than 15 seconds!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent gameIntent = new Intent(MenuActivity.this, LocalGameActivity.class);
                    gameIntent.putExtra("selectedSide", selectedSide);
                    gameIntent.putExtra("timeValueInMillis", timeValueInMillis);
                    gameIntent.putExtra("incrementValueInMillis", incrementValueInMillis);
                    gameIntent.putExtra("availableMovesColor", availableMovesColor);
                    gameIntent.putExtra("selectedPieceColor", selectedPieceColor);
                    startActivity(gameIntent);

                    popupDialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(MenuActivity.this, "Please enter valid time or increment values.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        popupDialog.show();
    }


    private void startGameAgainstBot(){
        final Dialog popupDialog = new Dialog(MenuActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth); // Use a theme without borders or padding
        popupDialog.setContentView(R.layout.activity_startgamewithbot);

        // Make sure the dialog takes up the full width of the screen
        popupDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        // Make the dialog's window transparent so your layout's background is visible
        popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Define the spinners from the layout
        Spinner spinner1 = popupDialog.findViewById(R.id.spinner1);
        Spinner spinner2 = popupDialog.findViewById(R.id.spinner2);
        Spinner spinner3 = popupDialog.findViewById(R.id.spinner3);

        Button startGame = popupDialog.findViewById(R.id.startGameButton);


        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedSide = spinner1.getSelectedItem().toString();
                String SelectedBotType = spinner2.getSelectedItem().toString();
                String selectedBotLevel = spinner3.getSelectedItem().toString();
                int botLevel = getBotLevel(selectedBotLevel);

                Intent gameIntent = new Intent(MenuActivity.this, ComputerGameActivity.class);

                gameIntent.putExtra("selectedSide", selectedSide);
                gameIntent.putExtra("selectedBotType", SelectedBotType);
                gameIntent.putExtra("selectedBotLevel", botLevel);

                startActivity(gameIntent);

                popupDialog.dismiss();
            }
        });


        popupDialog.show();
    }
    private int getBotLevel(String selectedBotLevel) {
        switch (selectedBotLevel) {
            case "level 1":
                return 1;
            case "level 2":
                return 2;
            case "level 3":
                return 3;
            case "level 4":
                return 4;
            default:
                return 1; // Default to level 1 if something goes wrong
        }
    }

    // Handles navigation item selections in the drawer menu
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            showToast("Home selected");
        } else if (id == R.id.nav_profile) {
            if (currentUser != null){
                startActivity(new Intent(MenuActivity.this, profileActivity.class));
            } else {
                showToast("You need to login");
                startActivity(new Intent(MenuActivity.this, LoginActivity.class));
            }
        } else if (id == R.id.nav_settings) {
            showToast("Settings selected");
            startActivity(new Intent(MenuActivity.this, SettingsActivity.class));
        } else if (id == R.id.nav_leaderBoard) {
            showToast("LeaderBoard selected");
            startActivity(new Intent(MenuActivity.this, LeaderBoardActivity.class));

        } else if (id == R.id.nav_localHistory) {
            showToast("Local History selected");
            startActivity(new Intent(MenuActivity.this, LocalHistoryActivity.class));
        } else if (id == R.id.nav_onlineHistory) {
            showToast("Online History selected");
            startActivity(new Intent(MenuActivity.this, OnlineHistoryActivity.class));
        }

        // Close the navigation drawer after selecting an item
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateSignInStatus();
    }


    // Removes extra game history rows on pause to prevent duplicate entries
    @Override
    protected void onPause() {
        super.onPause();
//        if (tableLayout.getChildCount() > 1) {
//            tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
//        }
    }


    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Handles the result returned from the started activity (for settings updates)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Update the color values from shared preferences after returning from SettingsActivity
            whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
            blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
            availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
            selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));
        }
    }
    private void updateSignInStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            signIn.setText("Sign In");
        } else {
            // fetch username or other info if necessary
            firebaseUtils.fetchUsername(user, username -> {
                if (username != null) {
                    signIn.setText(username);
                }
            });
        }
    }
}
