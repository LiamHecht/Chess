package com.example.chess.activities;

import android.content.Intent;
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

public class LocalHistoryActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private Button backButton;

    private FirebaseUtils firebaseUtils;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localgamehistory);

        tableLayout = findViewById(R.id.gameHistoryTable);
        backButton = findViewById(R.id.backButton);

        firebaseUtils = new FirebaseUtils();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        fetchAndDisplayGameHistory();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void addGameHistoryToTable(String winner, String totalMoves, String gameDate, List<String> pgnMoves) {
        TableRow tableRow = new TableRow(this);

        // Define margins
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(-16, 0, 0, 0);  // Negative left margin

        tableRow.setLayoutParams(layoutParams);
        tableRow.setPadding(0, 0, 0, 10);

        TextView winnerTextView = createTextView(winner);
        TextView totalMovesTextView = createTextView(totalMoves);
        TextView gameDateTextView = createTextView(gameDate);

        tableRow.addView(winnerTextView);
        tableRow.addView(totalMovesTextView);
        tableRow.addView(gameDateTextView);

        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.light_black));
        divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));

        TextView reviewTextView = createClickableTextView("Review", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reviewGame(winner, gameDate, totalMoves, pgnMoves);
            }
        });

        tableRow.addView(reviewTextView);

        tableLayout.addView(tableRow);
    }

    private TextView createClickableTextView(String text, View.OnClickListener onClickListener) {
        TextView textView = createTextView(text);
        textView.setTextColor(getResources().getColor(R.color.light_blue));
        textView.setOnClickListener(onClickListener);
        return textView;
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


    private void reviewGame(String winner, String gameDate, String totalMoves, List<String> pgnMoves) {
        Log.d("pgn", pgnMoves.toString());
        Intent intent = new Intent(LocalHistoryActivity.this, ReplayActivity.class);

        // Convert the list to an array
        String[] pgnArray = pgnMoves.toArray(new String[0]);

        intent.putExtra("pgmMoves", pgnArray);
        intent.putExtra("username", "white");
        intent.putExtra("opponentUsername", "black");

        startActivity(intent);
    }




    private void fetchAndDisplayGameHistory() {
        firebaseUtils.fetchLocalGameHistory(currentUser, gameHistories -> {
            for (Map<String, Object> gameHistory : gameHistories) {
                String winner = (String) gameHistory.get("winner");
                String gameDate = (String) gameHistory.get("gameDate");
                String totalMoves = String.valueOf(gameHistory.get("totalMoves"));
                List<String> pgnMoves = (List<String>) gameHistory.get("pgnMoves");

                addGameHistoryToTable(winner, totalMoves, gameDate, pgnMoves);
            }
        });
    }


}
