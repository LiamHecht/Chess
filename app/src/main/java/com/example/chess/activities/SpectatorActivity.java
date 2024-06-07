package com.example.chess.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chess.gameplay.Board;
import com.example.chess.gameplay.GameTimer;
import com.example.chess.gameplay.Move;
import com.example.chess.pieces.Piece;
import com.example.chess.R;
import com.example.chess.gameplay.Spot;
import com.example.chess.pieces.PieceType;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class SpectatorActivity extends BaseChess {

    private Socket socket; // Socket for network communication
    private BufferedReader in; // BufferedReader for reading data from the socket
    private PrintWriter out; // PrintWriter for writing data to the socket

    private String username; // Username of the spectator
    private boolean isCreatorWhite; // Flag indicating if the creator is playing with white pieces

    private String player1; // Name of the first player
    private String player2; // Name of the second player

    private TextView player1RatingLabel; // TextView to display the rating of the first player
    private TextView player2RatingLabel; // TextView to display the rating of the second player

    private final String serverIP = "35.246.192.221"; // IP address of the server
    private final int serverPort = 8080; // Port number of the server

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onlinegame);

        chessBoard = new Board(); // Initialize the chess board

        // Retrieve color settings from shared preferences
        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);
        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));

        // Retrieve player information from intent extras
        username = getIntent().getStringExtra("username");
        player1 = getIntent().getStringExtra("player1");
        player2 = getIntent().getStringExtra("player2");

        isCreatorWhite = getIntent().getBooleanExtra("isCreatorWhite", true);

        // Initialize UI components
        topTimerTextView = findViewById(R.id.topTimerTextView);
        bottomTimerTextView = findViewById(R.id.bottomTimerTextView);
        player1Label = findViewById(R.id.player1Label);
        player2Label = findViewById(R.id.player2Label);
        player1RatingLabel = findViewById(R.id.player1RatingLabel);
        player2RatingLabel = findViewById(R.id.player2RatingLabel);
        topFrame = findViewById(R.id.topTimerFrameLayout);
        bottomFrame = findViewById(R.id.bottomTimerFrameLayout);
        chessboardLayout = findViewById(R.id.chessboard);
        whiteTakenPiecesLayout = findViewById(R.id.whiteTakenPiecesLayout);
        blackTakenPiecesLayout = findViewById(R.id.blackTakenPiecesLayout);

        // Initialize timers
        whitePlayerTimer = new GameTimer(bottomTimerTextView);
        blackPlayerTimer = new GameTimer(topTimerTextView);

        // Set player labels based on creator's color
        if (isCreatorWhite) {
            player2Label.setText(player1);
            player1Label.setText(player2);
        } else {
            player1Label.setText(player1);
            player2Label.setText(player2);
        }

        initializeLayout();
        initializeChessboard();
        initiateConnection(); // Initiate the connection to the server

        // Hide the iconLinearLayout
        LinearLayout iconLinearLayout = findViewById(R.id.iconLinearLayout);
        iconLinearLayout.setVisibility(View.GONE);
    }

    @Override
    protected void handlePieceClick(int x, int y) {

    }

    @Override
    protected void selectPiece(int x, int y, Piece piece) {

    }

    @Override
    protected void handlePieceMove(int x, int y) {

    }

    protected void handlePieceMove(int x, int y, int selectedX, int selectedY) {
        PieceType promotionPiece = null;
        if (selectedPiece != null && selectedX != -1 && selectedY != -1) {
            Spot startSpot = chessBoard.getBox(selectedX, selectedY);
            Spot targetSpot = chessBoard.getBox(x, y);
            Piece potentialCapture = targetSpot.getPiece();

            if (isCastlingMove(x, y)) {
                int secx = x;
                int secy = y;
                showToast("Inside castle!");
                if (!chessBoard.performCastling(selectedX, selectedY, y > selectedY)) {
                    showToast("Cannot castle!");
                    return;
                }
                Boolean castlingSide = y > selectedY;
                postRecivedActions();
                return;
            }
            if (chessBoard.isEnPassant(selectedX, selectedY, x, y)) {
                showToast("Inside En passant");

                Spot adjacentPawnSpot = selectedPiece.isWhite() ? chessBoard.getBox(x + 1, y) : chessBoard.getBox(x - 1, y);
                Piece capturedPawn = adjacentPawnSpot.getPiece();
                int secx = selectedX;
                int secy = selectedY;
                if (!chessBoard.performEnPassant(selectedX, selectedY, x, y)) {
                    showToast("Cannot perform en passant!");
                    return;
                }

                capturePiece(capturedPawn);
                postRecivedActions();
                return;
            }


            if (!chessBoard.isValidMove(startSpot, targetSpot)) {
                showToast("Invalid move!");
                return;
            }

            Pair<Boolean, Boolean> simulationResults = chessBoard.simulateMove(selectedX, selectedY, x, y);
            boolean moveDoesNotExposeKing = simulationResults.first;
            boolean moveChecksOpponent = simulationResults.second;

            if (!moveDoesNotExposeKing) {
                showToast("Invalid move! You cannot put/leave your King in check.");
                return;
            }

            if (!chessBoard.movePiece(selectedX, selectedY, x, y)) {
                showToast("Invalid move!");
                return;
            }

            if (potentialCapture != null && potentialCapture != selectedPiece) {
                capturePiece(potentialCapture);
            }

            if (moveChecksOpponent) {
                showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is in check!");

                if (chessBoard.isCheckmate(!selectedPiece.isWhite())) {
                    initializeChessboard();
                    showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is checkmated! Game Over!");
                    return;
                }
            }

            if (chessBoard.isDraw()) {
                showToast("The game is a draw!");
                return;
            }

            PieceType promotionType = null;
            if (selectedPiece.getType() == PieceType.PAWN && ((selectedPiece.isWhite() && x == 0) || (!selectedPiece.isWhite() && x == 7))) {
                handleSpecialPieceConditions(x, y, selectedX, selectedY);  // Passed additional parameters for move
            } else {
                postRecivedActions();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        killConnection();
        // Perform any actions you want when this activity is closed
        Log.d("AppLifecycle", username);
    }


    private void initiateConnection() {
        new Thread(() -> {
            try {
                Socket socket = new Socket(serverIP, serverPort);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                listenForMoves();
                sendSpectatorMessageToServer();


            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error connecting to the server");
            }
        }).start();
    }

    private void sendSpectatorMessageToServer(){
        sendMessageToServer("spectator:"+username+":"+player1+"VS"+player2);

        try {
            Thread.sleep(500);  // Sleep for half a second
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void initiateUsersRatings(String receivedStr){
        String[] parts = receivedStr.split("-");
        String part1 = parts[0];
        String part2 = parts[1];
        if (part1.split(":")[0].equals(player1)){
            player1RatingLabel.setText(part1.split(":")[1]);
            player2RatingLabel.setText(part2.split(":")[1]);
        } else {
            player1RatingLabel.setText(part2.split(":")[1]);
            player2RatingLabel.setText(part1.split(":")[1]);
        }
    }

    private String moveToString(Move move) {
        return convertMoveToPgn(move.startX, move.startY, move.endX, move.endY, false, null);
    }
    private Move stringToMove(String moveStr) {
        char fileStart = moveStr.charAt(0);
        int rankStart = Character.getNumericValue(moveStr.charAt(1));
        char fileEnd = moveStr.charAt(2);
        int rankEnd = Character.getNumericValue(moveStr.charAt(3));

        int startX = 8 - rankStart;
        int startY = fileStart - 'a';
        int endX = 8 - rankEnd;
        int endY = fileEnd - 'a';

        return new Move(startX, startY, endX, endY);
    }


    private void sendMoveToServer(Move move) {
        sendMessageToServer("move:" + moveToString(move));
    }
    private void sendMessageToServer(String message) {
        new Thread(() -> {
            if (out != null) {
                out.println(message);
            }
        }).start();
    }
    private void listenForMoves() {
        Log.d("Received Move:", "listening");
        new Thread(() -> {
            try {
                while (true) {
                    String receivedStr = in.readLine();
                    Log.d("Received Move:", receivedStr);

                    if (receivedStr.contains("move_list:")){
                        receivedStr = receivedStr.replace("move_list:","");
//                        Log.d("move list", String.valueOf(moveList));
                        String finalReceivedStr = receivedStr;
                        runOnUiThread(() -> initBoardForSpectator(finalReceivedStr)); // handle the received message
                    }
                    else if (receivedStr.startsWith("ratings:")) {
                        receivedStr = receivedStr.replace("ratings:","");
                        String finalReceivedStr1 = receivedStr;
                        runOnUiThread(() -> initiateUsersRatings(finalReceivedStr1));

                    }
                    else if (receivedStr.contains("ping")){
                        receivedStr = receivedStr.replace("ping", "");
                        Log.d("Test", receivedStr);
                    }
                    else if (receivedStr.equals("gameStarted")) {

                    }

                    else if (receivedStr.startsWith("promotion:")) {
                        String[] splitData = receivedStr.split(":");
                        String[] coordinates = splitData[1].split(",");
                        int receivedX = Integer.parseInt(coordinates[0]);
                        int receivedY = Integer.parseInt(coordinates[1]);
                        PieceType promotionType = PieceType.valueOf(splitData[2]);
                        runOnUiThread(() -> handleReceivedPromotion(receivedX, receivedY, promotionType));
                    }

                    else if (receivedStr.startsWith("castling:")) {
                        String[] splitData = receivedStr.split(":");
                        int receivedX = Integer.parseInt(splitData[1]);
                        int receivedY = Integer.parseInt(splitData[2]);
                        boolean receivedIsKingside = Boolean.parseBoolean(splitData[3]);
                        runOnUiThread(() -> handleReceivedCastling(receivedX, receivedY, receivedIsKingside));
                    }
                    else if (receivedStr.startsWith("enpassant:")) {
                        String[] splitData = receivedStr.split(":");
                        int startX = Integer.parseInt(splitData[1]);
                        int startY = Integer.parseInt(splitData[2]);
                        int endX = Integer.parseInt(splitData[3]);
                        int endY = Integer.parseInt(splitData[4]);
                        runOnUiThread(() -> handleReceivedEnPassant(startX, startY, endX, endY));
                    }
                    else if (receivedStr.startsWith("move:")){
                        receivedStr = receivedStr.split(":")[1];
                        Move receivedMove = stringToMove(receivedStr);
                        runOnUiThread(() -> handleReceivedMove(receivedMove, null));
                    }

                    else if (receivedStr.startsWith("gameOver")) {
                        String reason = receivedStr.split(":")[1];
//                        finish();
                    }
                    else {
                        showToast("doesn't recognised");

                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection lost.", Toast.LENGTH_SHORT).show();
//                    showToast(e.toString());
//                    finish();
                    Log.d("error :", e.toString());

                });
            }
        }).start();
    }
    private void initBoardForSpectator(String receivedStr) {
        try {
            // Attempt to parse the string as JSON
            JSONArray moveList = new JSONArray(receivedStr);

            // If parsing succeeds, log that the string is JSON
            Log.d("json", "Received string is JSON");

            // Loop over the JSON array to process each move
            for (int i = 0; i < moveList.length(); i++) {
                String receivedMove = moveList.getString(i);
                Move move = stringToMove(receivedMove);
                Piece capturedPiece =  chessBoard.getBox(move.getEndX(), move.getEndY()).getPiece();
                if (capturedPiece != null){
                    capturePiece(capturedPiece);
                }
                chessBoard.placePieceWithoutValidation(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
//                handlePieceMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
            }
            initializeChessboard();
        } catch (JSONException e) {
            // If parsing fails, log that the string is not JSON
            Log.d("json", "Received string is not JSON");
        }
    }


    private void handleReceivedEnPassant(int startX, int startY, int endX, int endY) {
        boolean isSuccessful = chessBoard.performEnPassant(startX, startY, endX, endY);
        if (isSuccessful) {
            showToast("performed en passant!");
            postRecivedActions();
        } else {
            Log.e("OnlineGameActivity", "Failed to perform en passant after received move.");
        }
    }
    private void handleReceivedCastling(int x, int y, boolean isKingside) {
        boolean isSuccessful = chessBoard.performCastling(x,y, isKingside);
        if (isSuccessful) {
            String sideString = isKingside ? "kingside" : "queenside";
            showToast("Opponent castled " + sideString + "!");
            postRecivedActions();
        } else {
            Log.e("OnlineGameActivity", "Failed to perform castling after received move.");
        }
    }
    private void handleReceivedPromotion(int x, int y, PieceType promotionType) {
        if(promotionType != null) {
            chessBoard.promotePawn(x, y, promotionType);
            postRecivedActions();
        }
    }
    private void handleReceivedMove(Move move, PieceType promotionType) {
        if (move == null || move.startX < 0 || move.startY < 0 || move.endX < 0 || move.endY < 0 || move.startX > 7 || move.startY > 7 || move.endX > 7 || move.endY > 7) {
            return;
        }

        Spot startSpot = chessBoard.getBox(move.startX, move.startY);
        Spot endSpot = chessBoard.getBox(move.endX, move.endY);
        Piece potentialCapture = endSpot.getPiece();

        Log.d("Received Move:", "" + move.startX + "," + move.startY + ") to (" + move.endX + "," + move.endY + ")");
        showToast("Received Move: (" + move.startX + "," + move.startY + ") to (" + move.endX + "," + move.endY + ")");

        if (chessBoard.movePiece(move.startX, move.startY, move.endX, move.endY)) {

            // Check for piece capture
            if (potentialCapture != null && potentialCapture != startSpot.getPiece()) {
                capturePiece(potentialCapture);
                flipComponents();
            }
            if (chessBoard.isKingInCheck(!endSpot.getPiece().isWhite())) {
                if (chessBoard.isCheckmate(!endSpot.getPiece().isWhite())) {
                    initializeChessboard();
                    new Handler().postDelayed(() -> {
                        finish(); // Finish activity
                    }, 5000);
                }
            }
            initializeChessboard();
        } else {
            Log.e("OnlineGameActivity", "Failed to update board after received move.");
        }
        postRecivedActions();
    }
    private void postRecivedActions() {
        initializeChessboard();
        displayTakenPieces(whiteTakenPieces, whiteTakenPiecesLayout);
        displayTakenPieces(blackTakenPieces, blackTakenPiecesLayout);
        switchTimers();
    }

    private void killConnection() {
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void handleSquareClick(int x, int y) {

    }

    @Override
    protected void postMoveActions() {

    }

    @Override
    protected void handleGameOver(String winner) {

    }

    @Override
    protected void handleGameOver(String winner, String opponent, Boolean isWhite) {

    }

    @Override
    protected void handleSpecialPieceConditions(int x, int y, int selectedX, int selectedY) {

    }

    @Override
    protected void showPromotionDialog(int x, int y) {

    }

    @Override
    protected void flipComponents() {

    }
}
