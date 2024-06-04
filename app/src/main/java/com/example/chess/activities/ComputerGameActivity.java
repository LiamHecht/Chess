package com.example.chess.activities;

import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

import com.example.chess.gameplay.Board;
import com.example.chess.gameplay.ChessBot;
import com.example.chess.gameplay.ChessPgn;
import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.gameplay.Move;
import com.example.chess.gameplay.SoundManager;
import com.example.chess.gameplay.StockFishApi;
import com.example.chess.pieces.Bishop;
import com.example.chess.pieces.Knight;
import com.example.chess.pieces.Pawn;
import com.example.chess.pieces.Piece;
import com.example.chess.R;
import com.example.chess.gameplay.Spot;
import com.example.chess.pieces.King;
import com.example.chess.pieces.PieceType;
import com.example.chess.pieces.Queen;
import com.example.chess.pieces.Rook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ComputerGameActivity extends BaseChess {
    private ChessBot chessAI;
    private String selectedSide;
    private String selectedBotType;
    private boolean isWhitePlayer;
    private final Handler aiHandler = new Handler();
    private final Executor aiExecutor = Executors.newSingleThreadExecutor();
    private int depth;

    private StockFishApi stockFishAPI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //data base
        firestore = FirebaseFirestore.getInstance();
        firebaseUtils = new FirebaseUtils();

        soundManager = new SoundManager(this);
        chessBoard = new Board();
        chessPgn = new ChessPgn();


        player1Label = findViewById(R.id.player1Label);
        player2Label = findViewById(R.id.player2Label);

        player1Label.setText("bot");
        player2Label.setText("player");

        topTimerTextView = findViewById(R.id.topTimerTextView);
        bottomTimerTextView = findViewById(R.id.bottomTimerTextView);
        topTimerTextView.setText("00:00");
        bottomTimerTextView.setText("00:00");

        chessboardLayout = findViewById(R.id.chessboard);

        whiteTakenPiecesLayout = findViewById(R.id.whiteTakenPiecesLayout);
        blackTakenPiecesLayout = findViewById(R.id.blackTakenPiecesLayout);

        btnFlipBoard = findViewById(R.id.flipIcon);

        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);

        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));

        selectedSide = getIntent().getStringExtra("selectedSide");
        selectedBotType = getIntent().getStringExtra("selectedBotType");

        Log.d("side", "" + selectedSide);

        if (selectedSide.equals("white")){
            isWhitePlayer = true;
        } else if (selectedSide.equals("random")) {
            Random random = new Random();
            int randomValue = random.nextInt(2);

            isWhitePlayer = (randomValue == 0);
        }
        else{
            isWhitePlayer = false;
        }
        Log.d("side", "" + isWhitePlayer);

        depth = getIntent().getIntExtra("selectedBotLevel", 2);
        chessAI = new ChessBot(chessBoard, depth, isWhitePlayer);
        stockFishAPI = new StockFishApi(chessBoard,depth, isWhitePlayer);

        initializeLayout();
        initializeChessboard();

        btnFlipBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipBoard();
                flipComponents();
            }
        });
        if ("black".equals(selectedSide)) {
            if (selectedBotType.equals("MinMax")){
                aiMove();
            }
            else{
                performStockFishMove();
            }
        }
    }
    private void performStockFishMove(){
        stockFishAPI.sendStockfishRequest(new StockFishApi.OnMoveReceivedListener() {
            @Override
            public void onMoveReceived(String move) {
                runOnUiThread(() -> {
                    // Update the UI with the move
                    Move chessMove = convertPgnToMove(move);
                    // Proceed with the game logic
                    chessBoard.placePieceWithoutValidation(chessMove.startX, chessMove.startY, chessMove.endX, chessMove.endY);
                    chessBoard.toggleTurn();
                    postMoveActions();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showToast("Error fetching move from Stockfish: " + e.getMessage());
                });
            }
        });
    }



    protected void handleSquareClick(int x, int y) {
        if (chessBoard.isWhiteTurn() != isWhitePlayer) {
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
        if (chessBoard.isWhiteTurn() != isWhitePlayer) {
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
        if (chessBoard.isWhiteTurn() == isWhitePlayer) {
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
                chessPgn.addMove(convertMoveToPgn(secx, secy, x, y, false, null));
                capturePiece(capturedPawn);
                playSound(R.raw.chessgame_piecetakes);
                postMoveActions();
                return;
            }



            // check if the move is valid
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

            else if (potentialCapture != null && potentialCapture != selectedPiece) {
                capturePiece(potentialCapture);
                playSound(R.raw.chessgame_piecetakes);
                isPieceTaken = true;
            }


            if (moveChecksOpponent) {
                showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is in check!");

                if (chessBoard.isCheckmate(!selectedPiece.isWhite())) {
                    showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is checkmated! Game Over!");
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, null));
                    playSound(R.raw.chessgame_checkmate);
                    totalGameMoves++;
                    initializeChessboard();
                    handleGameOver(selectedPiece.isWhite() ? "white" : "black");
                    return;
                }
            }

            if (chessBoard.isDraw()) {
                showToast("The game is a draw!");
                playSound(R.raw.chessgame_stalemate);
                handleGameOver("draw");
            }
            PieceType promotionType = null;
            if (selectedPiece.getType() == PieceType.PAWN && ((selectedPiece.isWhite() && x == 0) || (!selectedPiece.isWhite() && x == 7))) {
                if (!selectedPiece.isWhite()){
                    chessBoard.promotePawn(x, y, PieceType.QUEEN);
                    promotionType = PieceType.QUEEN;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    promotionType = null;
                    postMoveActions();
                } else {
                    handleSpecialPieceConditions(x, y, selectedX, selectedY);
                }
            } else {
                chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                if (!isPieceTaken && !moveChecksOpponent){
                    playSound(R.raw.chessgame_piecemoves);
                }
                postMoveActions();
            }


        }
    }
    private void aiMove() {
        // Start the AI move calculations on a background thread.
        aiExecutor.execute(() -> {
            ChessBot.Move bestMove = chessAI.getBestMove();

            // After the calculation is done, post the move to the main thread to update the UI.
            aiHandler.post(() -> {
                if (bestMove == null) {
                    showToast("AI cannot find a move. Game might be over.");
                    return;
                }

                // Apply the best move on the board.
                Spot startSpot = bestMove.source;
                Spot endSpot = bestMove.destination;

                int startX = startSpot.getX();
                int startY = startSpot.getY();
                int endX = endSpot.getX();
                int endY = endSpot.getY();

                selectedPiece = startSpot.getPiece();
                selectedX = startX;
                selectedY = startY;

                handlePieceMove(endX, endY);
            });
        });
    }

    protected void postMoveActions() {
        clearAllHighlightsExceptSelected();
        clearSelection();
        initializeChessboard();

//        Log.d("FEN", sendStockfishRequest());
        flipComponents();
        totalGameMoves++;
        Log.d("test", selectedBotType);
        if (selectedBotType.equals("MinMax")){
            if (chessBoard.isWhiteTurn() != isWhitePlayer) {
                aiMove();
            }
        }
        else {
            if (chessBoard.isWhiteTurn() != isWhitePlayer){
                performStockFishMove();
            }
        }
    }
    protected void handleGameOver(String winner) {
        Map<String, Object> gameHistoryData = new HashMap<>();
        gameHistoryData.put("winner", winner);
        gameHistoryData.put("totalMoves", totalGameMoves);
        gameHistoryData.put("gameDate", getCurrentDate());
        gameHistoryData.put("pgnMoves", chessPgn.getPgnMoves());

        // Add game history to Firestore
        firebaseUtils.addLocalGameHistory(FirebaseAuth.getInstance().getCurrentUser(), gameHistoryData, isSuccess -> {
            if (isSuccess) {
                showToast("Game result successfully added to Firestore.");
            } else {
                showToast("Error adding game result to Firestore.");
            }
            new Handler().postDelayed(() -> {
                finish();
            }, 8000);
        });
    }
    protected void handleGameOver(String winner, String opponent, Boolean isWhite){

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
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });


            rookPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.ROOK);
                    promotionType = PieceType.ROOK;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });

            bishopPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.BISHOP);
                    promotionType = PieceType.BISHOP;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });


            knightPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.KNIGHT);
                    promotionType = PieceType.KNIGHT;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
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
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });


            rookPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.ROOK);
                    promotionType = PieceType.ROOK;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });

            bishopPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.BISHOP);
                    promotionType = PieceType.BISHOP;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });


            knightPromotion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chessBoard.promotePawn(x, y, PieceType.KNIGHT);
                    promotionType = PieceType.KNIGHT;
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                    popupDialog.dismiss();
                    promotionType = null;
                    postMoveActions();
                }
            });
        }
    }
    protected void flipComponents(){
        if (isBoardFlipped){
            player1Label.setText("player");
            player2Label.setText("bot");
//            displayTakenPieces(whiteTakenPieces, blackTakenPiecesLayout);
//            displayTakenPieces(blackTakenPieces, whiteTakenPiecesLayout);
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

        } else {
            player1Label.setText("bot");
            player2Label.setText("player");
//            displayTakenPieces(whiteTakenPieces, whiteTakenPiecesLayout);
//            displayTakenPieces(blackTakenPieces, blackTakenPiecesLayout);
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,whiteTakenPiecesLayout,blackTakenPiecesLayout);

        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
