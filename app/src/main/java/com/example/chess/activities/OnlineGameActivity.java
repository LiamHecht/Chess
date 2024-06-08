package com.example.chess.activities;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.chess.gameplay.Board;
import com.example.chess.gameplay.ChessPgn;
import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.gameplay.GameTimer;
import com.example.chess.gameplay.Move;
import com.example.chess.gameplay.SoundManager;
import com.example.chess.pieces.Piece;
import com.example.chess.R;
import com.example.chess.gameplay.Spot;
import com.example.chess.pieces.King;
import com.example.chess.pieces.PieceType;
import com.example.chess.pieces.Rook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class OnlineGameActivity extends BaseChess {

    private int lastMoveStartX = -1;
    private int lastMoveStartY = -1;
    private int lastMoveEndX = -1;
    private int lastMoveEndY = -1;

//    private PieceType promotionType;
    private FirebaseUser currentUser;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean isMyTurn;
    private boolean isWhitePlayer;
    private boolean isGameStarted = false;
    private boolean isGameOver;

    private String username;
    private String opponentUsername;

    private ImageView menuView;
    private ImageView btnFlipBoard;
    private ImageView btnChat;
//    private SharedPreferences sharedPreferences;
    private int currentRating;
    private int opponentRating;
    private TextView player1RatingLabel;
    private TextView player2RatingLabel;

    private Dialog chatDialog;
    private LinearLayout chatMessagesLayout;
    private Queue<String> messageQueue = new LinkedList<>();
    private List<String> sentMessages = new ArrayList<>();

    private final String serverIP = "10.0.2.2";
    private final int serverPort = 8080;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onlinegame);

        firestore = FirebaseFirestore.getInstance();
        firebaseUtils = new FirebaseUtils();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        chessPgn = new ChessPgn();
        chessBoard = new Board();
        soundManager = new SoundManager(this);
        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);
        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));
        username = getIntent().getStringExtra("username");
        opponentUsername = getIntent().getStringExtra("opponentUsername");

        isWhitePlayer = getIntent().getBooleanExtra("side", false);

        firebaseUtils.fetchUserRating(currentUser, currRating -> {
            Log.d("RatingFetched", "Rating: " + currRating);
            currentRating = currRating;
        });


        topTimerTextView = findViewById(R.id.topTimerTextView);
        bottomTimerTextView = findViewById(R.id.bottomTimerTextView);

        topFrame = findViewById(R.id.topTimerFrameLayout);
        bottomFrame = findViewById(R.id.bottomTimerFrameLayout);

        chessboardLayout = findViewById(R.id.chessboard);
        whiteTakenPiecesLayout = findViewById(R.id.whiteTakenPiecesLayout);
        blackTakenPiecesLayout = findViewById(R.id.blackTakenPiecesLayout);

        player1Label = findViewById(R.id.player1Label);
        player2Label = findViewById(R.id.player2Label);

        player1RatingLabel = findViewById(R.id.player1RatingLabel);
        player2RatingLabel = findViewById(R.id.player2RatingLabel);

        menuView = findViewById(R.id.menu);

        menuView.setOnClickListener(v -> {openMenu();});

        whitePlayerRemainingTime = getIntent().getLongExtra("timeValueInMillis", 180000);
        blackPlayerRemainingTime = getIntent().getLongExtra("timeValueInMillis", 180000);

        bonusTime = getIntent().getLongExtra("incrementValueInMillis", 0);

        whitePlayerTimer = new GameTimer(bottomTimerTextView);
        blackPlayerTimer = new GameTimer(topTimerTextView);

        btnFlipBoard = findViewById(R.id.flipIcon);
        btnFlipBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipBoard();
                flipComponents();
            }
        });

        btnChat = findViewById(R.id.chatIcon);

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChatDialog();

            }
        });


        initializeLayout();
        initializeChessboard();
        LinearLayout iconLinearLayout = findViewById(R.id.iconLinearLayout);

        whitePlayerTimer.setTimerListener(new GameTimer.TimerListener() {
            @Override
            public void onTimeOver() {
                if (isWhitePlayer){
                    handleGameOver("lose", opponentUsername, true);
                    Log.e("timer","time is up");
//                    sendGameOverMessage();
                }
                else {
                    handleGameOver("win", opponentUsername, false);
                }
            }
        });
        blackPlayerTimer.setTimerListener(new GameTimer.TimerListener() {
            @Override
            public void onTimeOver() {
                if (!isWhitePlayer){
                    handleGameOver("lose", opponentUsername, false);
                    Log.e("timer","time is up");
//                    sendGameOverMessage();
                }
                else {
                    handleGameOver("win", opponentUsername, true);
                }
            }
        });


        if (isWhitePlayer) {
            isMyTurn = true;

        } else {
            isMyTurn = false;
        }
        initiateConnection();
        // Start the timers with an initial time
        whitePlayerTimer.setInitialTime(whitePlayerRemainingTime);
        blackPlayerTimer.setInitialTime(blackPlayerRemainingTime);

    }
    private void addMessageToChatAndStore(String message, LinearLayout chatMessagesLayout) {
        TextView textView = new TextView(OnlineGameActivity.this);
        textView.setText(message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        chatMessagesLayout.addView(textView);

        // Store the message
        sentMessages.add(message);
    }

    // Method to display stored messages in the chat layout
    private void showChatDialog() {
        if (chatDialog == null || !chatDialog.isShowing()) {
            chatDialog = new Dialog(OnlineGameActivity.this);
            chatDialog.setContentView(R.layout.activity_chat);
            EditText messageInput = chatDialog.findViewById(R.id.messageInput);
            Button sendButton = chatDialog.findViewById(R.id.sendButton);
            chatMessagesLayout = chatDialog.findViewById(R.id.chatMessagesLayout);

            displayStoredMessages(chatMessagesLayout);

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = messageInput.getText().toString().trim();
                    message = username + ": " + message;
                    if (!message.isEmpty()) {
                        sendChatMessageToServer(message);

                        addMessageToChatAndStore(message, chatMessagesLayout);

                        messageInput.setText("");
                    }
                }
            });

            chatDialog.show();
        } else {
            chatDialog.show();
        }
    }

    private void displayStoredMessages(LinearLayout chatMessagesLayout) {
        for (String message : sentMessages) {
            addMessageToChat(message, chatMessagesLayout);
        }
    }

    private void handleReceivedMessage(String message) {
        if (chatMessagesLayout != null) {
            // If the chat dialog is open, add the message to the chat layout
            addMessageToChatAndStore(message, chatMessagesLayout);
        } else {
            sentMessages.add(message);
        }
    }


    private void addMessageToChat(String message, LinearLayout chatMessagesLayout) {
        TextView textView = new TextView(OnlineGameActivity.this);
        textView.setText(message);
        chatMessagesLayout.addView(textView);
    }

    protected void flipComponents(){
        if (isBoardFlipped){
            if (isWhitePlayer){
                player2Label.setText(opponentUsername);
                player1Label.setText(username);
                player2RatingLabel.setText(String.valueOf(opponentRating));
                player1RatingLabel.setText(String.valueOf(currentRating));

            } else{
                player2Label.setText(username);
                player1Label.setText(opponentUsername);
                player2RatingLabel.setText(String.valueOf(currentRating));
                player1RatingLabel.setText(String.valueOf(opponentRating));

            }
//            displayTakenPieces(whiteTakenPieces, blackTakenPiecesLayout);
//            displayTakenPieces(blackTakenPieces, whiteTakenPiecesLayout);
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

            topFrame.removeAllViews();
            bottomFrame.removeAllViews();
            topFrame.addView(bottomTimerTextView);
            bottomFrame.addView(topTimerTextView);

        } else{
            if (isWhitePlayer){
                player2Label.setText(username);
                player1Label.setText(opponentUsername);
                player2RatingLabel.setText(String.valueOf(currentRating));
                player1RatingLabel.setText(String.valueOf(opponentRating));

            } else{
                player2Label.setText(opponentUsername);
                player1Label.setText(username);
                player2RatingLabel.setText(String.valueOf(opponentRating));
                player1RatingLabel.setText(String.valueOf(currentRating));
            }
//            displayTakenPieces(whiteTakenPieces, whiteTakenPiecesLayout);
//            displayTakenPieces(blackTakenPieces, blackTakenPiecesLayout);
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,whiteTakenPiecesLayout,blackTakenPiecesLayout);

            topFrame.removeAllViews();
            bottomFrame.removeAllViews();
            topFrame.addView(topTimerTextView);
            bottomFrame.addView(bottomTimerTextView);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        killConnection();
        // Perform any actions you want when this activity is closed
        Log.d("AppLifecycle", username);
        if (!isGameOver){
            handleGameOver("lose", opponentUsername, isWhitePlayer);
        }
    }

    public void openMenu(){
        final Dialog popupDialog = new Dialog(OnlineGameActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
        popupDialog.setContentView(R.layout.activity_onlineoptions);
        Button forfeitButton = popupDialog.findViewById(R.id.button1);
        Button drawButton = popupDialog.findViewById(R.id.button2);
        Button giveTimerButton = popupDialog.findViewById(R.id.button3);
        popupDialog.show();
        forfeitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendForfeit();
                handleGameOver("lose", opponentUsername, isWhitePlayer);
                popupDialog.dismiss();
            }
        });

        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDraw();
                popupDialog.dismiss();
            }
        });
//
        giveTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateOpponentTimer();
                sendGiveTime(15);  // give 10 seconds to the opponent
                popupDialog.dismiss();
            }
        });

    }
    private void updateOwnTimer(){
        if (!isWhitePlayer){
            blackPlayerRemainingTime += 15 * 1000;
            blackPlayerTimer.stopTimer();
            blackPlayerTimer.updateTimerText();
            if (isMyTurn){
                blackPlayerTimer.startTimer(blackPlayerRemainingTime);
            }

        }
        else{
            whitePlayerRemainingTime += 15 * 1000;
            whitePlayerTimer.stopTimer();
            whitePlayerTimer.updateTimerText();
            if (isMyTurn){
                whitePlayerTimer.startTimer(whitePlayerRemainingTime);
            }
        }
    }
    private void updateOpponentTimer(){
        if (isWhitePlayer){
            blackPlayerRemainingTime += 15 * 1000;
            blackPlayerTimer.stopTimer();
            blackPlayerTimer.updateTimerText();
            if (!isMyTurn){
                blackPlayerTimer.startTimer(blackPlayerRemainingTime);
            }

        }
        else{
            whitePlayerRemainingTime += 15 * 1000;
            whitePlayerTimer.stopTimer();
            whitePlayerTimer.updateTimerText();
            if (!isMyTurn){
                whitePlayerTimer.startTimer(whitePlayerRemainingTime);
            }
        }
    }
    public void initiateConnection() {
        new Thread(() -> {
            try {
//                Socket socket = new Socket("35.234.99.17", 8080);
                Socket socket = new Socket(serverIP, serverPort);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                listenForMoves();

//                sendUsernameAndWait();
                sendInitialMessageToServer();
                sendRatingAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error connecting to the server");
            }
        }).start();
    }
    private void sendInitialMessageToServer() throws IOException {
        sendMessageToServer(username + "vs" + opponentUsername);
        try {
            Thread.sleep(500);  // Sleep for half a second
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendChatMessageToServer(String message){
        sendMessageToServer("chat:" + message);
    }
    private void sendUsernameAndWait() throws IOException {
        sendMessageToServer("username:" + username);
        try {
            Thread.sleep(500);  // Sleep for half a second
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void sendRatingAndWait() throws IOException {
        sendMessageToServer("rating:" + currentRating);
    }



    private void sendPromotionMessage(int x, int y, PieceType type) {
        sendMessageToServer("promotion:" + x + "," + y + ":" + type.name());
    }
    private void sendUsername() {
        sendMessageToServer("username:" + username);
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
    private void sendUserRatingToServer() {
        firebaseUtils.fetchUserRating(currentUser, currentRating -> {
            sendMessageToServer("rating:" + currentRating);
        });
    }

    private void sendMoveToServer(Move move) {
        sendMessageToServer("move:" + moveToString(move));
    }
    private void sendEnPassantToOpponent(int startX, int startY, int endX, int endY) {
        sendMessageToServer("enpassant:" + startX + ":" + startY + ":" + endX + ":" + endY);
    }

    private void sendGameOverMessage() {
        sendMessageToServer("gameOver:time");
    }
    private void sendForfeit() {
        sendMessageToServer("action:forfeit");
    }

    private void sendDraw() {
        sendMessageToServer("action:draw_offer");
        runOnUiThread(() -> Toast.makeText(this, "Draw offer sent.", Toast.LENGTH_SHORT).show());
    }
    private void sendGiveTime(int seconds) {
        sendMessageToServer("action:give_time:" + seconds);
    }


    private void sendCastlingToOpponent(int x, int y, boolean isKingSide) {
        sendMessageToServer("castling:" + x + ":" + y + ":" + isKingSide);
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
                    if (receivedStr.startsWith("username:")) {
                        opponentUsername = receivedStr.split(":")[1];
                        if (isWhitePlayer){
                            player2Label.setText(username);
                            player1Label.setText(opponentUsername);
                        } else{
                            player2Label.setText(opponentUsername);
                            player1Label.setText(username);
                        }
                    }
                    else if (receivedStr.contains("ping")){
                        receivedStr = receivedStr.replace("ping", "");
                        Log.d("Test", receivedStr);
                    }
                    else if (receivedStr.equals("gameStarted")) {
                        isGameStarted = true;
                        runOnUiThread(() -> {
                            playSound(R.raw.chessgame_opening);
                            whitePlayerTimer.startTimer(whitePlayerRemainingTime);
                        });

                    }
                    else if (receivedStr.startsWith("You win!")){
                        Log.d("Test", "INSIDE");
                        handleGameOver("win", opponentUsername, isWhitePlayer);
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
                        handleGameOver("win", opponentUsername, isWhitePlayer);

                    }
                    else if (receivedStr.startsWith("action:")) {
                        String action = receivedStr.split(":")[1];
                        switch (action) {
                            case "forfeit":
                                runOnUiThread(() -> {
                                    showToast("Your opponent has forfeited the game.");
                                    handleGameOver("win", opponentUsername, isWhitePlayer);
                                });
                                break;
                            case "draw_offer":
                                runOnUiThread(() -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                    builder.setTitle("Draw Offer");
                                    builder.setMessage("Your opponent has offered a draw. Do you accept?");
                                    builder.setPositiveButton("Yes", (dialog, which) -> {
                                        sendMessageToServer("action:draw_accept");
                                        handleGameOver("draw", opponentUsername, isWhitePlayer);
                                    });
                                    builder.setNegativeButton("No", (dialog, which) -> {
                                        sendMessageToServer("action:draw_decline");
                                    });
                                    builder.create().show();
                                });
                                break;
                            case "draw_accept":
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Draw accepted by opponent.", Toast.LENGTH_SHORT).show();
                                    handleGameOver("draw", opponentUsername, isWhitePlayer);
                                });
                                break;
                            case "draw_decline":
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Draw declined by opponent.", Toast.LENGTH_SHORT).show();
                                });
                                break;
                        }
                    }
                    else if (receivedStr.startsWith("action:give_time:")) {
                        runOnUiThread(this::updateOwnTimer);
                    }
                    else if (receivedStr.startsWith("rating:")) {
                        opponentRating = Integer.parseInt(receivedStr.split(":")[1]);
                        runOnUiThread(() -> {
//                            showToast("" + opponentRating);
                            if (player1RatingLabel != null && player2RatingLabel != null) {
                                if (isWhitePlayer) {
                                    player2RatingLabel.setText(String.valueOf(currentRating)); // Your rating
                                    player1RatingLabel.setText(String.valueOf(opponentRating)); // Opponent's rating
                                } else {
                                    player1RatingLabel.setText(String.valueOf(currentRating)); // Your rating
                                    player2RatingLabel.setText(String.valueOf(opponentRating)); // Opponent's rating
                                }
                            }
                        });
                    }
                    else if (receivedStr.startsWith("chat:")) {
                        String chatMessage = receivedStr.substring(5); // Extract the chat message
                        runOnUiThread(() -> handleReceivedMessage(chatMessage)); // handle the received message
                    }
                    else {
                        showToast("doesn't recognised");

                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
//                    Toast.makeText(this, "Connection lost.", Toast.LENGTH_SHORT).show();
//                    showToast(e.toString());
//                    finish();
                    Log.d("error :", e.toString());

                });
            }
        }).start();
    }
    private void handleReceivedEnPassant(int startX, int startY, int endX, int endY) {
        boolean isSuccessful = chessBoard.performEnPassant(startX, startY, endX, endY);
        if (isSuccessful) {
            showToast("Opponent performed en passant!");
            chessPgn.addMove(convertMoveToPgn(startX, startY, endX, endY, false, null));
            playSound(R.raw.chessgame_piecetakes);
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
            playSound(R.raw.chessgame_castle);
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
        boolean isPieceTaken = false;
        Spot startSpot = chessBoard.getBox(move.startX, move.startY);
        Spot endSpot = chessBoard.getBox(move.endX, move.endY);
        Piece potentialCapture = endSpot.getPiece();

        Log.d("Received Move:", "" + move.startX + "," + move.startY + ") to (" + move.endX + "," + move.endY + ")");
        showToast("Received Move: (" + move.startX + "," + move.startY + ") to (" + move.endX + "," + move.endY + ")");

        if (chessBoard.movePiece(move.startX, move.startY, move.endX, move.endY)) {

            // Check for piece capture
            if (potentialCapture != null && potentialCapture != startSpot.getPiece()) {
                capturePiece(potentialCapture);
                playSound(R.raw.chessgame_piecetakes);
                isPieceTaken = true;
//                flipComponents();
            }
            if (chessBoard.isKingInCheck(!endSpot.getPiece().isWhite())) {
                showToast("Your King is in check!");
                playSound(R.raw.chessgame_piecechecks);
                if (chessBoard.isCheckmate(!endSpot.getPiece().isWhite())) {
                    initializeChessboard();
                    chessPgn.addMove(convertMoveToPgn(move.startX, move.startY, move.endX, move.endY, false, null));
                    playSound(R.raw.chessgame_checkmate);
                    totalGameMoves++;
                    handleGameOver("lose", opponentUsername, isWhitePlayer);
                    return;
                }
            }
            initializeChessboard();
        } else {
            Log.e("OnlineGameActivity", "Failed to update board after received move.");
        }
        chessPgn.addMove(convertMoveToPgn(move.startX, move.startY, move.endX, move.endY, false, null));
        isMyTurn = true;
        if (!isPieceTaken){
            playSound(R.raw.chessgame_piecemoves);
        }
        postRecivedActions();
    }
    protected void handleSquareClick(int x, int y) {
        if (!isGameStarted){
            showToast("the game didnt start");
            return;
        }
        if (!isMyTurn) {
            showToast("It's not your turn!");
            return;
        }

        if (selectedPiece == null) {
            showToast("Select a piece to move first!");
        } else if (selectedX == x && selectedY == y) {
            clearSelection();
        } else {
            handlePieceMove(x, y);
        }
    }
    protected void handlePieceClick(int x, int y) {
        if (!isGameStarted){
            showToast("the game didnt start");
            return;
        }

        if (!isMyTurn) {
            showToast("It's not your turn!");
            return;
        }

        Spot spot = chessBoard.getBox(x, y);
        if (spot != null && spot.getPiece() != null) {
            Piece clickedPiece = spot.getPiece();
            if (selectedPiece == null) {
                selectPiece(x, y, clickedPiece);
                highlightAvailableMovesForPiece(x, y, clickedPiece);
            } else {
                // A piece is already selected
                if (clickedPiece.isWhite() == selectedPiece.isWhite()) {
                    // Clicked on the player's own piece, switch selection
                    selectPiece(x, y, clickedPiece);
                    highlightAvailableMovesForPiece(x, y, clickedPiece);
                } else {
                    handlePieceMove(x, y);
                }
            }
        }
    }
    protected void selectPiece(int x, int y, Piece piece) {
        if (!isGameStarted){
            showToast("the game didnt start");
            return;
        }

        if (piece.isWhite() == chessBoard.isWhiteTurn()) {
            clearSelection();
            selectedX = x;
            selectedY = y;
            selectedPiece = piece;
            highlightAvailableMovesForPiece(x, y, piece);
            highlightSelectedSquare(x, y);
        } else {
            showToast("It's not your turn!");
        }
    }

    protected void handlePieceMove(int x, int y) {
        PieceType promotionPiece = null;
        boolean isPieceTaken = false;

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
                sendCastlingToOpponent(selectedX, selectedY, y > selectedY);
                chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, secx, secy, true, null));
                playSound(R.raw.chessgame_castle);
                postMoveActions();
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
                playSound(R.raw.chessgame_piecetakes);
                sendEnPassantToOpponent(secx, secy, x, y);
                chessPgn.addMove(convertMoveToPgn(secx, secy, x, y, false, null));
                postMoveActions();
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
                playSound(R.raw.chessgame_piecetakes);
                isPieceTaken = true;
            }

            if (moveChecksOpponent) {
                showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is in check!");
                playSound(R.raw.chessgame_piecechecks);
                if (chessBoard.isCheckmate(!selectedPiece.isWhite())) {
                    initializeChessboard();
                    sendMoveToServer(new Move(selectedX, selectedY, x, y));
                    showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is checkmated! Game Over!");
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, null));
                    playSound(R.raw.chessgame_checkmate);
                    totalGameMoves++;

                    handleGameOver("win", opponentUsername, isWhitePlayer);
                    return;
                }
            }

            if (chessBoard.isDraw()) {
                showToast("The game is a draw!");
                playSound(R.raw.chessgame_stalemate);
                return;
            }

            lastMoveStartX = selectedX;
            lastMoveStartY = selectedY;
            lastMoveEndX = x;
            lastMoveEndY = y;
            PieceType promotionType = null;
            if (selectedPiece.getType() == PieceType.PAWN && ((selectedPiece.isWhite() && x == 0) || (!selectedPiece.isWhite() && x == 7))) {
                handleSpecialPieceConditions(x, y, selectedX, selectedY);  // Passed additional parameters for move
            } else {
                chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                if (!isPieceTaken && !moveChecksOpponent){
                    playSound(R.raw.chessgame_piecemoves);
                }
                postMoveActions();
            }
            isMyTurn = false;

        }
    }
    protected void postMoveActions() {
        clearAllHighlightsExceptSelected();
        clearSelection();
        initializeChessboard();
//        displayTakenPieces(whiteTakenPieces, whiteTakenPiecesLayout);
//        displayTakenPieces(blackTakenPieces, blackTakenPiecesLayout);
        if (isBoardFlipped){
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

        } else {

            displayTakenPieces(whiteTakenPieces,blackTakenPieces,whiteTakenPiecesLayout,blackTakenPiecesLayout);
        }
        switchTimers();
        totalGameMoves ++;
        sendMoveToServer(new Move(lastMoveStartX, lastMoveStartY, lastMoveEndX, lastMoveEndY));
    }

    private void postRecivedActions() {
        clearAllHighlightsExceptSelected();
        clearSelection();
        initializeChessboard();
//        displayTakenPieces(whiteTakenPieces, whiteTakenPiecesLayout);
//        displayTakenPieces(blackTakenPieces, blackTakenPiecesLayout);
        if (isBoardFlipped){
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

        } else {

            displayTakenPieces(whiteTakenPieces,blackTakenPieces,whiteTakenPiecesLayout,blackTakenPiecesLayout);
        }
        switchTimers();
        totalGameMoves ++;
    }
    protected void handleGameOver(String winner){

    }
    protected void handleGameOver(String result, String opponent, Boolean isWhite) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Error: User not found.");
            return;
        }
        whitePlayerTimer.stopTimer();
        blackPlayerTimer.stopTimer();
        isGameOver = true;


        // Fetch current rating and games
        firebaseUtils.fetchUserRating(currentUser, currentRating -> {
            firebaseUtils.fetchUserGames(currentUser, currentGames -> {

                int updatedRating;
                if (currentRating != null) {
                    if (result.equals("win")) {
                        updatedRating = currentRating + 10;
                    } else if (result.equals("lose")) {
                        updatedRating = currentRating - 10; // Decrement for a loss
                    } else {
                        updatedRating = currentRating;
                    }
                } else {
                    updatedRating = 0; // or any default value you choose
                }

                int updatedGames = (currentGames != null) ? currentGames + 1 : 1;
                String playAs = isWhite ? "white" : "black";
                Map<String, Object> credentialsToUpdate = new HashMap<>();
                credentialsToUpdate.put("rating", updatedRating);
                credentialsToUpdate.put("games", updatedGames);

                // Update the user's rating and games
                firebaseUtils.updateCredentials(currentUser, credentialsToUpdate, isUpdated -> {
                    if (isUpdated) {
                        showToast("User's credentials updated successfully.");
                    } else {
                        showToast("Error updating user's credentials.");
                    }
                });

                sendMessageToServer("gameOver");
                Map<String, Object> gameHistoryData = new HashMap<>();
                gameHistoryData.put("result", result);
                gameHistoryData.put("opponent", opponent);
                gameHistoryData.put("playAs", playAs);
                gameHistoryData.put("totalMoves", totalGameMoves);
                gameHistoryData.put("gameDate", getCurrentDate());
                gameHistoryData.put("pgnMoves", chessPgn.getPgnMoves());

                // Add game history to Firestore
                firebaseUtils.addOnlineGameHistory(currentUser, gameHistoryData, isSuccess -> {
                    if (isSuccess) {
                        showToast("Game result successfully added to Firestore.");
                    } else {
                        showToast("Error adding game result to Firestore.");
                    }

                    new Handler().postDelayed(() -> {
                        finish(); // Finish activity
                    }, 5000);
                });
            });
        });
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

    protected void handleSpecialPieceConditions(int x, int y, int selectedX, int selectedY) {
        // Check for pawn promotion
        if (selectedPiece.getType() == PieceType.PAWN && selectedPiece.isWhite() && x == 0){
            showToast("promoted");

            showPromotionDialog(x, y);

        }
        if (selectedPiece.getType() == PieceType.PAWN && !selectedPiece.isWhite() && x == 7){
            showToast("promoted");

            showPromotionDialog(x, y);

        }
        if (selectedPiece.getType() == PieceType.ROOK && !((Rook) selectedPiece).hasMoved()) {
            ((Rook) selectedPiece).setMoved(true);
        }
        if (selectedPiece.getType() == PieceType.KING && !((King) selectedPiece).hasMoved()) {
            ((King) selectedPiece).setMoved(true);
        }
        initializeChessboard();
    }
    protected void showPromotionDialog(int x, int y) {

        if (selectedPiece.isWhite()){
            final Dialog popupDialog = new Dialog(this);
            popupDialog.setContentView(R.layout.activity_choosewhitepiece);

            ImageView queenPromotion = popupDialog.findViewById(R.id.queenPromotion);
            ImageView rookPromotion = popupDialog.findViewById(R.id.rookPromotion);
            ImageView bishopPromotion = popupDialog.findViewById(R.id.bishopPromotion);
            ImageView knightPromotion = popupDialog.findViewById(R.id.knightPromotion);
            popupDialog.show();

            queenPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.QUEEN);
                    promotionType = PieceType.QUEEN;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.QUEEN);
                    popupDialog.dismiss();
                }
            });


            rookPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.ROOK);
                    promotionType = PieceType.ROOK;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.ROOK);
                    popupDialog.dismiss();
                }
            });

            bishopPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.BISHOP);
                    promotionType = PieceType.BISHOP;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.BISHOP);
                    popupDialog.dismiss();
                }
            });


            knightPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.KNIGHT);
                    promotionType = PieceType.KNIGHT;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.KNIGHT);
                    popupDialog.dismiss();
                }
            });


        }
        else{

            final Dialog popupDialog = new Dialog(this);
            popupDialog.setContentView(R.layout.activity_chooseblackpiece);

            ImageView queenPromotion = popupDialog.findViewById(R.id.queenPromotion);
            ImageView rookPromotion = popupDialog.findViewById(R.id.rookPromotion);
            ImageView bishopPromotion = popupDialog.findViewById(R.id.bishopPromotion);
            ImageView knightPromotion = popupDialog.findViewById(R.id.knightPromotion);
            popupDialog.show();

            queenPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.QUEEN);
                    promotionType = PieceType.QUEEN;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.QUEEN);
                    popupDialog.dismiss();
                }
            });


            rookPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.ROOK);
                    promotionType = PieceType.ROOK;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.ROOK);
                    popupDialog.dismiss();
                }
            });

            bishopPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.BISHOP);
                    promotionType = PieceType.BISHOP;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.BISHOP);
                    popupDialog.dismiss();
                }
            });


            knightPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.KNIGHT);
                    promotionType = PieceType.KNIGHT;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                    sendPromotionMessage(x,y,PieceType.KNIGHT);
                    popupDialog.dismiss();
                }
            });
        }
    }
}
