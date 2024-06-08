package com.example.chess.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;

public class OnlineHistoryActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private Button backButton;
    private String username;

    private FirebaseUser currentUser;
    private FirebaseUtils firebaseUtils;

    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onlinegamehistory);

        tableLayout = findViewById(R.id.gameHistoryTable);
        backButton = findViewById(R.id.backButton);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUtils = new FirebaseUtils();

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String currentUsername = sharedPreferences.getString("loggedInUser", null);
        if (currentUsername != null){
            username = currentUsername;
        } else{
            firebaseUtils.fetchUsername(currentUser, currUsername -> {
                if (currUsername != null) {
                    username = currUsername;
                }
            });
        }

        fetchAndDisplayGameHistory();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private void addGameHistoryToTable(String winner, String opponent, String totalMoves, String gameDate, List<String> pgnMoves, String playAs) {
        TableRow tableRow = new TableRow(this);

        // Define margins
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(-16, 0, 0, 0);

        tableRow.setLayoutParams(layoutParams);
        tableRow.setPadding(0, 0, 0, 10);

        TextView winnerTextView = createTextView(winner);
        TextView opponentTextView = createTextView(opponent);
        TextView totalMovesTextView = createTextView(totalMoves);
        TextView gameDateTextView = createTextView(gameDate);

        tableRow.addView(winnerTextView);
        tableRow.addView(opponentTextView);
        tableRow.addView(totalMovesTextView);
        tableRow.addView(gameDateTextView);

        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.light_black));
        divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));

        tableLayout.addView(divider);

        // Create a review "link" using TextView
        TextView reviewTextView = createClickableTextView("Review", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reviewGame(winner, gameDate, opponent, totalMoves, pgnMoves, playAs);
            }
        });

        tableRow.addView(reviewTextView);

        tableLayout.addView(tableRow);
    }

    private void fetchAndDisplayGameHistory() {
        firebaseUtils.fetchOnlineGameHistory(currentUser, gameHistories -> {
            for (Map<String, Object> gameHistory : gameHistories) {
                String winner = (String) gameHistory.get("result");
                String opponent = (String) gameHistory.get("opponent");
                String gameDate = (String) gameHistory.get("gameDate");
                String totalMoves = String.valueOf(gameHistory.get("totalMoves"));
                List<String> pgnMoves = (List<String>) gameHistory.get("pgnMoves");
                String playAs = (String) gameHistory.get("playAs");  // Fetch "playAs"

                addGameHistoryToTable(winner, opponent, totalMoves, gameDate, pgnMoves, playAs); // Pass "playAs"
            }
        });
    }

    private void reviewGame(String winner, String gameDate, String opponent, String totalMoves, List<String> pgnMoves, String playAs) {
        Log.d("pgn", pgnMoves.toString());
        Intent intent = new Intent(OnlineHistoryActivity.this, ReplayActivity.class);

        // Convert the list to an array
        String[] pgnArray = pgnMoves.toArray(new String[0]);

        intent.putExtra("pgmMoves", pgnArray);
        intent.putExtra("username", username);
        intent.putExtra("opponentUsername", opponent);
        intent.putExtra("playAs", playAs);

        startActivity(intent);
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
    private TextView createClickableTextView(String text, View.OnClickListener onClickListener) {
        TextView textView = createTextView(text);
        textView.setTextColor(getResources().getColor(R.color.light_blue));
        textView.setOnClickListener(onClickListener);
        return textView;
    }


}
