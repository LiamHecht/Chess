package com.example.chess.activities;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

import com.example.chess.gameplay.Board;
import com.example.chess.gameplay.ChessPgn;
import com.example.chess.firebase.FirebaseUtils;
import com.example.chess.gameplay.GameTimer;
import com.example.chess.gameplay.SoundManager;
import com.example.chess.pieces.Piece;
import com.example.chess.R;
import com.example.chess.gameplay.Spot;
import com.example.chess.pieces.King;
import com.example.chess.pieces.PieceType;
import com.example.chess.pieces.Rook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LocalGameActivity extends BaseChess {
    private ImageView menuView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();
        firebaseUtils = new FirebaseUtils();

        // Initialize sound manager for sound effects
        soundManager = new SoundManager(this);

        // Initialize the chessboard and PGN (Portable Game Notation) for game tracking
        chessBoard = new Board();
        chessPgn = new ChessPgn();

        // Set player labels
        player1Label = findViewById(R.id.player1Label);
        player2Label = findViewById(R.id.player2Label);

        // Assign player colors
        player1Label.setText("black");
        player2Label.setText("white");

        // Initialize the timer layouts
        topFrame = findViewById(R.id.topTimerFrameLayout);
        bottomFrame = findViewById(R.id.bottomTimerFrameLayout);

        // Initialize the timer text views
        topTimerTextView = findViewById(R.id.topTimerTextView);
        bottomTimerTextView = findViewById(R.id.bottomTimerTextView);

        // Initialize the chessboard layout
        chessboardLayout = findViewById(R.id.chessboard);

        // Initialize the taken pieces layouts
        whiteTakenPiecesLayout = findViewById(R.id.whiteTakenPiecesLayout);
        blackTakenPiecesLayout = findViewById(R.id.blackTakenPiecesLayout);

        // Initialize the button to flip the chessboard
        btnFlipBoard = findViewById(R.id.flipIcon);

        // Initialize the menu view
        menuView = findViewById(R.id.menu);

        // Load preferences for chessboard colors and available move colors
        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);
        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#FF7F7F"));

        // Get the initial timer values and bonus time from the intent
        whitePlayerRemainingTime = getIntent().getLongExtra("timeValueInMillis", 180000);
        blackPlayerRemainingTime = getIntent().getLongExtra("timeValueInMillis", 180000);
        bonusTime = getIntent().getLongExtra("incrementValueInMillis", 0);

        // Initialize the timers for both players
        whitePlayerTimer = new GameTimer(bottomTimerTextView);
        blackPlayerTimer = new GameTimer(topTimerTextView);

        // Set up the layout and chessboard
        initializeLayout();
        initializeChessboard();

        // Start the timers with the initial time
        whitePlayerTimer.startTimer(whitePlayerRemainingTime);
        blackPlayerTimer.setInitialTime(blackPlayerRemainingTime);

        // Play the opening sound
        playSound(R.raw.chessgame_opening);

        // Set listeners for timer expiration to handle game over scenarios
        whitePlayerTimer.setTimerListener(new GameTimer.TimerListener() {
            @Override
            public void onTimeOver() {
                handleGameOver("black");
            }
        });
        blackPlayerTimer.setTimerListener(new GameTimer.TimerListener() {
            @Override
            public void onTimeOver() {
                handleGameOver("white");
            }
        });

        // Set up the button to flip the chessboard and its components
        btnFlipBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipBoard();
                flipComponents();
            }
        });
    }

    protected void handleSquareClick(int x, int y) {
        if (selectedPiece == null) {
            showToast("Select a piece to move first!");
        } else if (selectedX == x && selectedY == y) {
            clearSelection();
        } else {
            handlePieceMove(x, y);
        }
    }


    protected void handlePieceClick(int x, int y) {
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
        boolean isPieceTaken = false;
        if (selectedPiece != null && selectedX != -1 && selectedY != -1) {
            Spot startSpot = chessBoard.getBox(selectedX, selectedY);
            Spot targetSpot = chessBoard.getBox(x, y);
            Piece potentialCapture = targetSpot.getPiece();
            // Check if the move is a castling move
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
                playSound(R.raw.chessgame_piecechecks);
                if (chessBoard.isCheckmate(!selectedPiece.isWhite())) {
                    showToast((selectedPiece.isWhite() ? "Black" : "White") + " King is checkmated! Game Over!");
                    chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, null));
                    playSound(R.raw.chessgame_checkmate);
                    totalGameMoves++;
                    handleGameOver(selectedPiece.isWhite() ? "white" : "black");
                }
            }

            if (chessBoard.isDraw()) {
                showToast("The game is a draw!");
                playSound(R.raw.chessgame_stalemate);
                handleGameOver("draw");
            }
            PieceType promotionType = null;
            if (selectedPiece.getType() == PieceType.PAWN && ((selectedPiece.isWhite() && x == 0) || (!selectedPiece.isWhite() && x == 7))) {
                handleSpecialPieceConditions(x, y, selectedX, selectedY);
            } else {
                chessPgn.addMove(convertMoveToPgn(selectedX, selectedY, x, y, false, promotionType));
                if (!isPieceTaken && !moveChecksOpponent){
                    playSound(R.raw.chessgame_piecemoves);
                }
                postMoveActions();
            }


        }
    }

    protected void postMoveActions() {
        clearAllHighlightsExceptSelected();
        clearSelection();
        initializeChessboard();
        if (isBoardFlipped){
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

        } else {

            displayTakenPieces(whiteTakenPieces,blackTakenPieces,whiteTakenPiecesLayout,blackTakenPiecesLayout);
        }
        switchTimers();
        totalGameMoves ++;
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
                    promotionType = null;  // Reset it
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
    protected void flipComponents() {
        if (isBoardFlipped) {
            player1Label.setText("white");
            player2Label.setText("black");
//            displayTakenPieces(whiteTakenPieces, blackTakenPiecesLayout);
//            displayTakenPieces(blackTakenPieces, whiteTakenPiecesLayout);
            displayTakenPieces(whiteTakenPieces,blackTakenPieces,blackTakenPiecesLayout,whiteTakenPiecesLayout);

            topFrame.removeAllViews();
            bottomFrame.removeAllViews();
            topFrame.addView(bottomTimerTextView);
            bottomFrame.addView(topTimerTextView);
        } else {
            player1Label.setText("black");
            player2Label.setText("white");
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

        whitePlayerTimer.stopTimer();
        blackPlayerTimer.stopTimer();
    }
}
