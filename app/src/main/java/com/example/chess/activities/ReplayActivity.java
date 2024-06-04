package com.example.chess.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chess.gameplay.Board;
import com.example.chess.gameplay.ChessPgn;
import com.example.chess.gameplay.Move;
import com.example.chess.gameplay.MoveHistory;
import com.example.chess.pieces.Piece;
import com.example.chess.R;
import com.example.chess.gameplay.SpecialMoveType;
import com.example.chess.gameplay.Spot;
import com.example.chess.pieces.King;
import com.example.chess.pieces.Pawn;
import com.example.chess.pieces.PieceType;
import com.example.chess.pieces.Rook;

import org.checkerframework.checker.index.qual.LengthOf;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class ReplayActivity extends AppCompatActivity {
    private ChessPgn chessPgn;
    private Board chessBoard;
    private int currentMoveIndex = 0;
    private List<String> pgnMoves;

    private int whiteSideSquareColor;
    private int blackSideSquareColor;

    private String username = "";
    private String opponentUsername = "";

    private TextView whiteLabel;
    private TextView blackLabel;
    private ImageView nextMoveButton;
    private ImageView previousMoveButton;
    private Deque<MoveHistory> moveHistories = new ArrayDeque<>();
    private LinearLayout whiteMovesLayout;
    private LinearLayout blackMovesLayout;
    private String playedAs;
    private GridLayout chessboardLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay);

        nextMoveButton = findViewById(R.id.nextMoveButton);
        previousMoveButton = findViewById(R.id.previousMoveButton);

        whiteMovesLayout = findViewById(R.id.whiteMovesLayout);
        blackMovesLayout = findViewById(R.id.blackMovesLayout);


        whiteLabel = findViewById(R.id.whiteLabel);
        blackLabel = findViewById(R.id.blackLabel);
        chessboardLayout = findViewById(R.id.chessboard);

        // Initialize chess game and board here
        chessPgn = new ChessPgn();
        chessBoard = new Board();



        whiteSideSquareColor = getIntent().getIntExtra("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = getIntent().getIntExtra("blackSideSquareColor", Color.parseColor("#E0E0E0"));

        username = getIntent().getStringExtra("username");
        opponentUsername = getIntent().getStringExtra("opponentUsername");

        playedAs = getIntent().getStringExtra("playAs");

        setPlayerLabels();

        String[] pgnArray = getIntent().getStringArrayExtra("pgmMoves");
        List<String> pgnMoves = Arrays.asList(pgnArray);
        chessPgn.setPgnMoves(pgnMoves);


        initializeLayout();
        initializeChessboard();

        nextMoveButton.setOnClickListener(view -> showNextMove());
        previousMoveButton.setOnClickListener(view -> showPreviousMove());
    }
    private void setPlayerLabels() {
        if ("white".equalsIgnoreCase(playedAs)) {
            whiteLabel.setText(username);
            blackLabel.setText(opponentUsername);
        } else {
            blackLabel.setText(username);
            whiteLabel.setText(opponentUsername);
        }
    }
    private void logEntireMoveHistory() {
        StringBuilder historyBuilder = new StringBuilder();
        for (MoveHistory moveHistory : moveHistories) {
            historyBuilder.append(moveHistory.getMove().toString()).append(", ");
        }
        Log.d("ReplayActivity", "Move History: " + historyBuilder.toString());
    }

    private void showNextMove() {
        if (chessPgn.getPgnMoves() == null || currentMoveIndex < 0 || currentMoveIndex >= chessPgn.getPgnMoves().size()) {
            showToast("No more moves.");
            return;
        }

        String nextMove = chessPgn.getPgnMoves().get(currentMoveIndex);
        Move move = parsePgnMove(nextMove);

        Piece movingPiece = chessBoard.getBox(move.getStartX(), move.getStartY()).getPiece();

        // Check if the movingPiece is null and log accordingly
        if(movingPiece == null) {
            Log.e("ReplayActivity", "Error: Moving piece is null for move: " + nextMove);
            return;
        } else {
            Log.d("ReplayActivity", "Moving piece: " + movingPiece.toString());
        }
        Log.d("next Move in pgn list", nextMove.toString());
        applyMove(move);
        currentMoveIndex++;
        initializeChessboard();
        logEntireMoveHistory();

        LinearLayout moveEntryLayout = new LinearLayout(this);
        moveEntryLayout.setOrientation(LinearLayout.HORIZONTAL);

        ImageView pieceImageView = new ImageView(this);
        pieceImageView.setImageResource(getMicroPieceImageResource(movingPiece));
        moveEntryLayout.addView(pieceImageView);

        TextView moveTextView = new TextView(this);
        moveTextView.setText(nextMove);
        moveEntryLayout.addView(moveTextView);

        // If it's a white move
        if (currentMoveIndex % 2 == 1) {
            whiteMovesLayout.addView(moveEntryLayout);
        } else {
            blackMovesLayout.addView(moveEntryLayout);
        }

    }

    private void applyMove(Move move) {
        Piece capturedPiece = chessBoard.getBox(move.getEndX(), move.getEndY()).getPiece();
        MoveHistory moveHistory = new MoveHistory(move, capturedPiece); // Create the MoveHistory object here, but update it later if needed
        Log.d("moveHistory",moveHistory.toString());
        if (chessBoard.isCastlingMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY())) {
            Log.d("ReplayActivity", "castle");
            boolean isKingSide = move.getEndY() == 6;
            chessBoard.performCastling(move.getStartX(), move.getStartY(), isKingSide);
            moveHistory.setSpecialMoveType(SpecialMoveType.CASTLING); // Set the castling special move type here
        }
        else if (chessBoard.isEnPassant(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY())) {
            int capturedX = move.getStartX();
            int capturedY = move.getEndY();
            capturedPiece = chessBoard.getBox(capturedX, capturedY).getPiece();

            if (capturedPiece == null) {
                Log.e("ReplayActivity", "Expected to capture a pawn with en passant, but found no piece at: " + capturedX + "," + capturedY);
                return;
            }

            moveHistory = new MoveHistory(move, capturedPiece);
            // Set the en passant special move type here.
            moveHistory.setSpecialMoveType(SpecialMoveType.EN_PASSANT);

            // Create a new MoveHistory with the move and the captured pawn.
            moveHistories.push(moveHistory); // Save the move and the captured pawn to the history.

            // Perform the en passant move
            chessBoard.performEnPassant(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
            return;
        }
        else {
            chessBoard.movePiece(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
            if (move.getPromotionType() != null) {
                chessBoard.promotePawn(move.getEndX(), move.getEndY(), move.getPromotionType());
                moveHistory.setSpecialMoveType(SpecialMoveType.PROMOTION);

            }
        }

        moveHistories.push(moveHistory);
    }


    private Move parsePgnMove(String pgnMove) {
        PieceType promotionType = null;

        if (pgnMove.contains("=")) {
            char promoPiece = pgnMove.charAt(pgnMove.indexOf("=") + 1);
            promotionType = getPieceTypeFromChar(promoPiece);
            pgnMove = pgnMove.split("=")[0];
        }

        if ("O-O".equals(pgnMove) || "O-O-O".equals(pgnMove)) {
            int x, y, newX, newY;
//            boolean isWhite = chessBoard.isWhiteTurn();
            boolean isWhite = chessPgn.getLength() % 2 == 0 ? false : true;
            if (isWhite) {
                x = 7;
                y = 4;
            } else {
                x = 0;
                 y = 4;
            }
            newX = x;
            newY = "O-O".equals(pgnMove) ? 6 : 2;
            Move move = new Move(x, y, newX, newY);
            move.setPromotionType(promotionType);
            return move;
        }

        if (pgnMove.length() != 4) {
            throw new IllegalArgumentException("Invalid PGN move format: " + pgnMove);
        }

        char fileStart = pgnMove.charAt(0);
        char rankStartChar = pgnMove.charAt(1);
        char fileEnd = pgnMove.charAt(2);
        char rankEndChar = pgnMove.charAt(3);

        int y = fileStart - 'a';
        int x = 8 - (rankStartChar - '0');
        int newY = fileEnd - 'a';
        int newX = 8 - (rankEndChar - '0');

        Move move = new Move(x, y, newX, newY);
        move.setPromotionType(promotionType);
        return move;
    }

    private void showPreviousMove() {
        if (moveHistories.isEmpty()) {
            showToast("No previous moves.");
            return;
        }

        MoveHistory lastMoveHistory = moveHistories.peek();
        Log.d("Move in history", lastMoveHistory.getMove().toString());
        revertMove();

        // If it was a white move
        if (currentMoveIndex % 2 == 1) {
            if (whiteMovesLayout.getChildCount() > 0) {
                whiteMovesLayout.removeViewAt(whiteMovesLayout.getChildCount() - 1);
            }
        } else {
            if (blackMovesLayout.getChildCount() > 0) {
                blackMovesLayout.removeViewAt(blackMovesLayout.getChildCount() - 1);
            }
        }


        currentMoveIndex--;
        logEntireMoveHistory();
    }



    public void revertMove() {
        if (moveHistories.isEmpty()) {
            showToast("No previous moves to revert.");
            return;
        }

        MoveHistory lastMoveHistory = moveHistories.pop();
        Move move = lastMoveHistory.getMove();


        Piece pieceToRevert = chessBoard.getBox(move.getEndX(), move.getEndY()).getPiece();

        if(pieceToRevert instanceof Pawn) {
            Log.d("ReplayActivity", "Reverting pawn move from (" + move.getEndX() + ", " + move.getEndY() + ") to (" + move.getStartX() + ", " + move.getStartY() + ")");
        }

        // Handle special moves
        if (lastMoveHistory.getSpecialMoveType() == SpecialMoveType.CASTLING) {
            reverseCastling(move);
        }
        else if (lastMoveHistory.getSpecialMoveType() == SpecialMoveType.EN_PASSANT) {
            reverseEnPassant(move, lastMoveHistory.getCapturedPiece());

        }
        else {
            if (lastMoveHistory.getSpecialMoveType() == SpecialMoveType.PROMOTION) {
                reversePromotion(move);
            }
            // Move the piece back to its original position
            chessBoard.placePieceWithoutValidation(move.getEndX(), move.getEndY(), move.getStartX(), move.getStartY());

            // If there was a piece captured in the last move, restore it
            Piece capturedPiece = lastMoveHistory.getCapturedPiece();
            if (capturedPiece != null) {
                chessBoard.setBox(move.getEndX(), move.getEndY(), capturedPiece);
            }
        }

        // Update the chessboard after move has been reverted
        initializeChessboard();
    }

    private void reverseEnPassant(Move move, Piece capturedPiece) {
        int capturedX = move.getStartX();
        int capturedY = move.getEndY();
        Spot movingPawnSpot = chessBoard.getBox(move.getEndX(), move.getEndY());

        if (movingPawnSpot == null || movingPawnSpot.getPiece() == null) {
            Log.e("ReplayActivity", "movingPawnSpot or its piece is null!");
            return;
        }

        Spot startSpot = chessBoard.getBox(move.getStartX(), move.getStartY());
        Spot capturedPawnSpot = chessBoard.getBox(capturedX, capturedY);

        capturedPawnSpot.setPiece(capturedPiece);      // Restore the captured pawn to its original position
        startSpot.setPiece(movingPawnSpot.getPiece()); // Move the capturing pawn back to its original position
        movingPawnSpot.setPiece(null);                 // The spot where the capturing pawn ended up after the en passant is now empty
    }

    private void reversePromotion(Move move) {
        showToast("Reverted a promotion move!");
        int startX = move.getStartX();
        int startY = move.getStartY();
        int endX = move.getEndX();
        int endY = move.getEndY();
//        Log.d("Replay", startX + " " + startY);
////        Log.d("Replay", chessBoard.getBox(startX,startY).getPiece().toString());
//        Log.d("Replay", endX + " " + endY);
//        Log.d("Replay", chessBoard.getBox(endX,endY).getPiece().toString());
        Piece currentPiece = chessBoard.getBox(endX,endY).getPiece();
        chessBoard.getBox(endX,endY).setPiece(new Pawn(currentPiece.isWhite()));
//        Spot endSpot = chessBoard.getBox(endX, endY);
//        endSpot.setPiece(new Pawn(endSpot.getPiece().isWhite()));
    }


    private void reverseCastling(Move move) {
        // Check if it's a King-side or Queen-side castling based on the king's movement
        boolean isKingside = move.getEndY() == 6;

        int rookOriginalY;
        int rookFinalY;

        if (isKingside) {
            // King-side castling
            rookOriginalY = 7; // original position of the rook for kingside castling
            rookFinalY = 5;    // rook's position after kingside castling
        } else {
            // Queen-side castling
            rookOriginalY = 0; // original position of the rook for queenside castling
            rookFinalY = 3;    // rook's position after queenside castling
        }

        // Move rook back to its original position
        chessBoard.placePieceWithoutValidation(move.getStartX(), rookFinalY, move.getStartX(), rookOriginalY);

        // Get the King and set its hasMoved property to false
        Piece king = chessBoard.getBox(move.getEndX(), move.getEndY()).getPiece();
        if (king instanceof King) {
            ((King) king).setMoved(false);
        }

        // Get the Rook and set its hasMoved property to false
        Piece rook = chessBoard.getBox(move.getStartX(), rookOriginalY).getPiece();
        if (rook instanceof Rook) {
            ((Rook) rook).setMoved(false);
        }

        // Move the King back to its original position
        chessBoard.placePieceWithoutValidation(move.getEndX(), move.getEndY(), move.getStartX(), move.getStartY());
    }


    private PieceType getPieceTypeFromChar(char pieceChar) {
        switch (pieceChar) {
            case 'Q': return PieceType.QUEEN;
            case 'R': return PieceType.ROOK;
            case 'N': return PieceType.KNIGHT;
            case 'B': return PieceType.BISHOP;
            default: throw new IllegalArgumentException("Invalid promotion piece: " + pieceChar);
        }
    }


    protected void initializeLayout(){
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                FrameLayout square = new FrameLayout(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(i, 1f);
                params.columnSpec = GridLayout.spec(j, 1f);
                square.setLayoutParams(params);
                square.setBackgroundColor((i + j) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor);
                chessboardLayout.addView(square);

                final int x = i;
                final int y = j;

            }
        }
    }
    protected void initializeChessboard() {
        String[] columnLabels = {"a", "b", "c", "d", "e", "f", "g", "h"};
        String[] rowLabels = {"8", "7", "6", "5", "4", "3", "2", "1"};


        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int logicalX = x;
                int logicalY = y;

                FrameLayout square = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(x * 8 + y);

                // Set the square's background color
                int backgroundColor = (x + y) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor;
                square.setBackgroundColor(backgroundColor);

                // Clear the square by removing any existing piece images
                square.removeAllViews();

                Spot spot = chessBoard.getBox(logicalX, logicalY);
                if (spot != null && spot.getPiece() != null) {
                    ImageView pieceImageView = new ImageView(this);
                    pieceImageView.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    Piece piece = spot.getPiece();
                    int pieceImageResource = getPieceImageResource(piece);
                    pieceImageView.setImageResource(pieceImageResource);

                    square.addView(pieceImageView);

                }
            }
        }
    }


    private void displayTakenPieces(List<Piece> takenPieces, LinearLayout takenPiecesLayout) {
        takenPiecesLayout.removeAllViews();

        for (Piece piece : takenPieces) {
            ImageView pieceImageView = new ImageView(this);

            int resourceId = getMicroPieceImageResource(piece);

            pieceImageView.setImageResource(resourceId);

            takenPiecesLayout.addView(pieceImageView);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addLabelToSquare(FrameLayout square, String text1, String text2, int gravity) {
        LinearLayout labelLayout = new LinearLayout(this);
        labelLayout.setOrientation(LinearLayout.VERTICAL);

        if (text1 != null) {
            TextView label1 = new TextView(this);
            label1.setText(text1);
            label1.setTextColor(Color.BLACK);
            labelLayout.addView(label1, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM));
        }

        if (text2 != null) {
            TextView label2 = new TextView(this);
            label2.setText(text2);
            label2.setTextColor(Color.BLACK);
            labelLayout.addView(label2, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP));
        }

        square.addView(labelLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));
    }

    private int getPieceImageResource(Piece piece) {
        int resourceId = 0;

        PieceType pieceType = piece.getType();
        boolean isWhite = piece.isWhite();

        switch (pieceType) {
            case PAWN:
                resourceId = isWhite ? R.drawable.wpawn : R.drawable.bpawn;
                break;
            case ROOK:
                resourceId = isWhite ? R.drawable.wrook : R.drawable.brook;
                break;
            case KNIGHT:
                resourceId = isWhite ? R.drawable.wknight : R.drawable.bknight;
                break;
            case BISHOP:
                resourceId = isWhite ? R.drawable.wbishop : R.drawable.bbishop;
                break;
            case QUEEN:
                resourceId = isWhite ? R.drawable.wqueen : R.drawable.bqueen;
                break;
            case KING:
                resourceId = isWhite ? R.drawable.wking : R.drawable.bking;
                break;
        }

        return resourceId;
    }

    private int getMicroPieceImageResource(Piece piece) {
        int resourceId = 0;

        PieceType pieceType = piece.getType();
        boolean isWhite = piece.isWhite();

        switch (pieceType) {
            case PAWN:
                resourceId = isWhite ? R.drawable.wp : R.drawable.bp;
                break;
            case ROOK:
                resourceId = isWhite ? R.drawable.wr : R.drawable.br;
                break;
            case KNIGHT:
                resourceId = isWhite ? R.drawable.wn : R.drawable.bn;
                break;
            case BISHOP:
                resourceId = isWhite ? R.drawable.wb : R.drawable.bb;
                break;
            case QUEEN:
                resourceId = isWhite ? R.drawable.wq : R.drawable.bq;
                break;
            case KING:
                resourceId = isWhite ? R.drawable.wk : R.drawable.bk;
                break;
        }

        return resourceId;
    }
}