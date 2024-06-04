package com.example.chess.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseChess extends AppCompatActivity {

    // classes
    protected Board chessBoard;
    protected ChessPgn chessPgn;
    protected SoundManager soundManager;
    //game logic
    protected int selectedX = -1;
    protected int selectedY = -1;
    protected Piece selectedPiece = null;

    //layout variables
    protected GridLayout chessboardLayout;
    protected GameTimer whitePlayerTimer;
    protected GameTimer blackPlayerTimer;
    protected TextView topTimerTextView;
    protected TextView bottomTimerTextView;
    protected TextView player1Label;
    protected TextView player2Label;
    protected FrameLayout topFrame;
    protected FrameLayout bottomFrame;
    protected ImageView btnFlipBoard;

    //display taken pieces
    protected List<Piece> whiteTakenPieces = new ArrayList<>();
    protected List<Piece> blackTakenPieces = new ArrayList<>();


    protected LinearLayout blackTakenPiecesLayout;

    protected LinearLayout whiteTakenPiecesLayout;

    //handle time variables
    protected long whitePlayerRemainingTime = 180000; // 3 minutes in milliseconds
    protected long blackPlayerRemainingTime = 180000;

    protected long bonusTime;

    // board style
    protected int whiteSideSquareColor;
    protected int blackSideSquareColor;
    protected int availableMovesColor;
    protected int selectedPieceColor;
    protected int totalGameMoves = 0;
    protected SharedPreferences sharedPreferences;



    // fireBase variables
    protected FirebaseFirestore firestore;
    protected FirebaseUtils firebaseUtils;

    //flip board
    protected boolean isBoardFlipped = false;

    //handle promotion
    protected PieceType promotionType = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    protected abstract void handlePieceClick(int x, int y);
    protected void clearSelection() {
        if (selectedX != -1 && selectedY != -1) {
            FrameLayout selectedSquare = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(selectedX * 8 + selectedY);
            selectedSquare.setBackgroundColor((selectedX + selectedY) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor);
        }
        selectedX = -1;
        selectedY = -1;
        selectedPiece = null;

    }
    protected abstract void selectPiece(int x, int y, Piece piece);

    protected abstract void handlePieceMove(int x, int y);
    protected abstract void handleSquareClick(int x, int y) ;
    protected abstract void postMoveActions();
    protected String convertMoveToPgn(int startX, int startY, int endX, int endY, Boolean isCastle, PieceType promotionType) {
        if (isCastle) {
            if (endY > 4) {
                return "O-O"; // Kingside castling
            } else {
                return "O-O-O"; // Queenside castling
            }
        }

        char fileStart = (char) ('a' + startY);
        char fileEnd = (char) ('a' + endY);
        int rankStart = 8 - startX;
        int rankEnd = 8 - endX;

        String move = "" + fileStart + rankStart + fileEnd + rankEnd;

        if (promotionType != null) {
            char promoChar = getCharFromPieceType(promotionType);
            move += "=" + promoChar;
        }

        return move;
    }
    protected Move convertPgnToMove(String pgnMove) {
        if (pgnMove == null) {
            // Handle the case where pgnMove is null, perhaps logging an error or throwing an exception
            Log.e("convertPgnToMove", "Received null pgnMove string.");
            throw new IllegalArgumentException("Invalid PGN move format: " + pgnMove);
        }
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

    protected char getCharFromPieceType(PieceType type) {
        switch (type) {
            case QUEEN: return 'Q';
            case ROOK: return 'R';
            case BISHOP: return 'B';
            case KNIGHT: return 'N';
            default: throw new IllegalArgumentException("Invalid piece type");
        }
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

    protected abstract void handleGameOver(String winner);
    protected abstract void handleGameOver(String winner, String opponent, Boolean isWhite);
    protected abstract void handleSpecialPieceConditions(int x, int y, int selectedX, int selectedY);
    protected abstract void showPromotionDialog(int x, int y);
    protected void switchTimers() {
        if (chessBoard.isWhiteTurn()) {
            // Pause Black's timer and resume White's timer
            blackPlayerRemainingTime = blackPlayerTimer.pauseTimer();
            blackPlayerRemainingTime += bonusTime;
            whitePlayerTimer.startTimer(whitePlayerRemainingTime);
        } else {
            // Pause White's timer and resume Black's timer
            whitePlayerRemainingTime = whitePlayerTimer.pauseTimer();
            whitePlayerRemainingTime += bonusTime;
            blackPlayerTimer.startTimer(blackPlayerRemainingTime);
        }
    }
//-----------------------------------------------------------------------------------------------------------------------
    protected boolean isCastlingMove(int x, int y) {
        if (selectedPiece instanceof King && Math.abs(selectedY - y) == 2 && !((King) selectedPiece).hasMoved()) {
            int rookColumn = (y > selectedY) ? 7 : 0;
            Piece potentialRook = chessBoard.getBox(x, rookColumn).getPiece();
            return potentialRook instanceof Rook && !((Rook) potentialRook).hasMoved();
        }
        return false;
    }
    protected void capturePiece(Piece captured) {
        if (captured.isWhite()) {
            blackTakenPieces.add(captured);
        } else {
            whiteTakenPieces.add(captured);
        }
    }
    protected void highlightSelectedSquare(int x, int y) {
        FrameLayout selectedSquare = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(x * 8 + y);
        selectedSquare.setBackgroundColor(selectedPieceColor);
    }
    protected void highlightSquare(int x, int y) {
        FrameLayout square = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(x * 8 + y);
        square.setBackgroundColor(availableMovesColor);
    }
    protected void highlightAvailableMovesForPiece(int x, int y, Piece piece) {
        PieceType pieceType = piece.getType();
        clearAllHighlightsExceptSelected();

        boolean isWhite = piece.isWhite();

        switch (pieceType) {
            case ROOK:
                highlightRookMoves(x, y);
                break;
            case PAWN:
                highlightPawnMoves(x, y, isWhite);
                break;
            case KNIGHT:
                highlightKnightMoves(x, y);
                break;
            case BISHOP:
                highlightBishopMoves(x, y);
                break;
            case QUEEN:
                highlightQueenMoves(x, y);
                break;
            case KING:
                highlightKingMoves(x, y);
                break;
            default:
                break;
        }
    }
    protected void clearAllHighlightsExceptSelected() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i != selectedX || j != selectedY) {  // skip the selected square
                    FrameLayout square = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(i * 8 + j);
//                    square.setBackgroundResource((i + j) % 2 == 0 ? R.color.white : R.color.light_gray);
                    square.setBackgroundColor((i + j) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor);

                }
            }
        }
    }
    protected void highlightRookMoves(int x, int y) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // right, left, down, up

        for (int[] direction : directions) {
            int nx = x + direction[0];
            int ny = y + direction[1];
            while (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                Spot spot = chessBoard.getBox(nx, ny);
                if (spot == null || spot.getPiece() == null) {
                    highlightSquare(nx, ny);
                } else {
                    if (spot.getPiece().isWhite() != chessBoard.getBox(x, y).getPiece().isWhite()) {
                        highlightSquare(nx, ny);  // highlight if opponent piece
                    }
                    break;  // stop if there's a piece blocking
                }
                nx += direction[0];
                ny += direction[1];
            }
        }
    }
    protected void highlightPawnMoves(int x, int y, boolean isWhite) {
        if (!isWhite) {
            if (x < 7) { // Move forward for white pawns
                highlightSquare(x + 1, y);
                if (x == 1){
                    highlightSquare(x + 2, y); // Double move on initial move
                }
                if (y > 0 && isSquareOccupiedByOpponent(x + 1, y - 1, isWhite)){
                    highlightSquare(x + 1, y - 1); // Capture left
                }
                if (y < 7 && isSquareOccupiedByOpponent(x + 1, y + 1, isWhite)){
                    highlightSquare(x + 1, y + 1); // Capture right
                }
            }
        } else {
            if (x > 0) { // Move forward for black pawns
                highlightSquare(x - 1, y);
                if (x == 6){
                    highlightSquare(x - 2, y); // Double move on initial move
                }
                if (y > 0 && isSquareOccupiedByOpponent(x - 1, y - 1, isWhite)){
                    highlightSquare(x - 1, y - 1); // Capture left
                }
                if (y < 7 && isSquareOccupiedByOpponent(x - 1, y + 1, isWhite)){
                    highlightSquare(x - 1, y + 1); // Capture right
                }
            }
        }
    }
    protected boolean isSquareOccupiedByOpponent(int x, int y, boolean isWhite) {
        Spot spot = chessBoard.getBox(x, y);
        if (spot != null && spot.getPiece() != null) {
            Piece piece = spot.getPiece();
            return piece.isWhite() != isWhite;
        }
        return false;
    }
    protected void highlightKnightMoves(int x, int y) {
        int[] dx = {1, 1, 2, 2, -1, -1, -2, -2};
        int[] dy = {2, -2, 1, -1, 2, -2, 1, -1};

        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                Spot spot = chessBoard.getBox(nx, ny);
                if (spot == null || spot.getPiece() == null) {
                    highlightSquare(nx, ny);
                } else if (spot.getPiece().isWhite() != chessBoard.getBox(x, y).getPiece().isWhite()) {
                    highlightSquare(nx, ny);
                }
            }
        }
    }
    protected void highlightBishopMoves(int x, int y) {
        int[][] directions = {{1, 1}, {-1, -1}, {1, -1}, {-1, 1}};  // down-right, up-left, down-left, up-right

        for (int[] direction : directions) {
            int nx = x + direction[0];
            int ny = y + direction[1];
            while (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                Spot spot = chessBoard.getBox(nx, ny);
                if (spot == null || spot.getPiece() == null) {
                    highlightSquare(nx, ny);
                } else {
                    if (spot.getPiece().isWhite() != chessBoard.getBox(x, y).getPiece().isWhite()) {
                        highlightSquare(nx, ny);
                    }
                    break;
                }
                nx += direction[0];
                ny += direction[1];
            }
        }
    }
    protected void highlightQueenMoves(int x, int y) {
        highlightRookMoves(x, y);
        highlightBishopMoves(x, y);
    }
    protected void highlightKingMoves(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                        Spot spot = chessBoard.getBox(nx, ny);
                        boolean canMove = false;
                        // Check if the square is empty or has an opponent's piece.
                        if (spot == null || spot.getPiece() == null || spot.getPiece().isWhite() != chessBoard.getBox(x, y).getPiece().isWhite()) {
                            // Simulate the King's move to check if the King would be safe after the move.
                            Pair<Boolean, Boolean> simulationResults = chessBoard.simulateMove(x, y, nx, ny);
                            canMove = simulationResults.first;
                        }
                        // If move is valid and doesn't put King in check, then highlight the square.
                        if (canMove) {
                            highlightSquare(nx, ny);
                        }
                    }
                }
            }
        }
    }
    //--------------------------------------------------------------------------------------------
    protected String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    protected int getPieceImageResource(Piece piece) {
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
    protected int getMicroPieceImageResource(Piece piece) {
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
    protected void printTakenPieces() {
        Log.d("taken", "White Taken Pieces:");
        for (Piece piece : whiteTakenPieces) {
            Log.d("taken",piece.getType() + " " + (piece.isWhite() ? "White" : "Black"));
        }

        Log.d("taken", "Black Taken Pieces:");
        for (Piece piece : blackTakenPieces) {
            Log.d("taken", piece.getType() + " " + (piece.isWhite() ? "White" : "Black"));
        }
    }
    protected void playSound(int soundResourceId){
        soundManager.playSound(soundResourceId);
    }
    protected void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    protected void initializeLayout(){
        // Loop through each row
        for (int i = 0; i < 8; i++) {
            // Loop through each column
            for (int j = 0; j < 8; j++) {
                // Create a new FrameLayout for each square
                FrameLayout square = new FrameLayout(this);

                // Set layout parameters for the square
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;  // Use 0 for width to allow weight to handle sizing
                params.height = 0; // Use 0 for height to allow weight to handle sizing
                params.rowSpec = GridLayout.spec(i, 1f);  // Row spec with weight 1
                params.columnSpec = GridLayout.spec(j, 1f);  // Column spec with weight 1

                // Apply the layout parameters to the square
                square.setLayoutParams(params);

                // Set the background color based on the position
                square.setBackgroundColor((i + j) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor);

                // Add the square to the chessboard layout
                chessboardLayout.addView(square);

                // Final variables for use in the click listener
                final int x = i;
                final int y = j;

                // Set an OnClickListener for the square
                square.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle the square click event
                        handleSquareClick(x, y);
                    }
                });
            }
        }
    }

    protected void initializeChessboard() {
        // Define labels for the columns and rows
        String[] columnLabels = {"a", "b", "c", "d", "e", "f", "g", "h"};
        String[] rowLabels = {"8", "7", "6", "5", "4", "3", "2", "1"};

        // Loop through each position on the board
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int logicalX = x; // Logical X-coordinate
                int logicalY = y; // Logical Y-coordinate

                // Get the FrameLayout for the current square
                FrameLayout square = (FrameLayout) ((GridLayout) findViewById(R.id.chessboard)).getChildAt(x * 8 + y);

                // Set the background color based on the position
                int backgroundColor = (x + y) % 2 == 0 ? whiteSideSquareColor : blackSideSquareColor;
                square.setBackgroundColor(backgroundColor);

                // Clear any existing views (e.g., piece images) from the square
                square.removeAllViews();

                // Get the spot on the chessboard
                Spot spot = chessBoard.getBox(logicalX, logicalY);

                // If the spot is occupied by a piece, display the piece
                if (spot != null && spot.getPiece() != null) {
                    ImageView pieceImageView = new ImageView(this);
                    pieceImageView.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    // Get the piece and its image resource
                    Piece piece = spot.getPiece();
                    int pieceImageResource = getPieceImageResource(piece);
                    pieceImageView.setImageResource(pieceImageResource);

                    // Rotate the piece image if the board is flipped
                    pieceImageView.setRotation(isBoardFlipped ? 180 : 0);

                    // Add the piece image to the square
                    square.addView(pieceImageView);

                    // Set an OnClickListener for the piece
                    pieceImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Handle the piece click event
                            handlePieceClick(logicalX, logicalY);
                        }
                    });
                }

                // Add column label if this is the last row
                if (x == 7) {
                    addLabelToSquare(square, columnLabels[y], null, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                }

                // Add row label if this is the last column
                if (y == 7) {
                    addLabelToSquare(square, null, rowLabels[x], Gravity.END | Gravity.CENTER_VERTICAL);
                }
            }
        }
    }


    protected void flipBoard() {
        isBoardFlipped = !isBoardFlipped; // Toggle the board flip state
        chessboardLayout.setRotation(isBoardFlipped ? 180 : 0);
        initializeChessboard();
    }

    protected void addLabelToSquare(FrameLayout square, String text1, String text2, int gravity) {
        LinearLayout labelLayout = new LinearLayout(this);
        labelLayout.setOrientation(LinearLayout.VERTICAL);

        // Set alignment based on the text presence and specific positions
        int horizontalGravity = (text1 != null) ? Gravity.CENTER_HORIZONTAL : Gravity.END;
        int verticalGravity = (text2 != null) ? Gravity.CENTER_VERTICAL : Gravity.BOTTOM;

        if (text1 != null) {
            TextView label1 = new TextView(this);
            label1.setText(text1);
            label1.setTextColor(Color.BLACK);
            label1.setGravity(horizontalGravity); // Center horizontally if it's column label
            label1.setRotation(isBoardFlipped ? 180 : 0);  // Rotate the label based on the flip state
            labelLayout.addView(label1);
        }

        if (text2 != null) {
            TextView label2 = new TextView(this);
            label2.setText(text2);
            label2.setTextColor(Color.BLACK);
            label2.setGravity(verticalGravity);
            label2.setRotation(isBoardFlipped ? 180 : 0);
            labelLayout.addView(label2);
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = gravity;

        square.addView(labelLayout, layoutParams);
    }


    protected void displayTakenPieces(List<Piece> takenPieces, LinearLayout takenPiecesLayout) {
        takenPiecesLayout.removeAllViews();

        for (Piece piece : takenPieces) {
            ImageView pieceImageView = new ImageView(this);

            int resourceId = getMicroPieceImageResource(piece);

            pieceImageView.setImageResource(resourceId);

            takenPiecesLayout.addView(pieceImageView);
        }
    }
    protected void displayTakenPieces(List<Piece> whiteTakenPieces, List<Piece> blackTakenPieces, LinearLayout whiteTakenPiecesLayout, LinearLayout blackTakenPiecesLayout) {
        whiteTakenPiecesLayout.removeAllViews();
        blackTakenPiecesLayout.removeAllViews();

        // Map to hold the value of each piece type
        Map<PieceType, Integer> pieceValueMap = new HashMap<>();
        pieceValueMap.put(PieceType.PAWN, 1);
        pieceValueMap.put(PieceType.KNIGHT, 3);
        pieceValueMap.put(PieceType.BISHOP, 3);
        pieceValueMap.put(PieceType.ROOK, 5);
        pieceValueMap.put(PieceType.QUEEN, 9);
        // King is not included as it's invaluable

        int whiteTotalValue = calculateTotalValue(whiteTakenPieces, pieceValueMap);
        int blackTotalValue = calculateTotalValue(blackTakenPieces, pieceValueMap);

        // Calculate the advantage
        int advantage = whiteTotalValue - blackTotalValue;

        // Determine which side has the advantage
        boolean whiteAdvantage = advantage > 0;
        boolean blackAdvantage = advantage < 0;

        // Display taken pieces for white side
        displayPieces(whiteTakenPieces, whiteTakenPiecesLayout);

        // Display taken pieces for black side
        displayPieces(blackTakenPieces, blackTakenPiecesLayout);

        // Create and add the total value TextView for the side with an advantage
        if (whiteAdvantage) {
            TextView totalValueTextView = new TextView(this);
            totalValueTextView.setText("+" + advantage);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            totalValueTextView.setLayoutParams(params);

            whiteTakenPiecesLayout.addView(totalValueTextView);
        } else if (blackAdvantage) {
            TextView totalValueTextView = new TextView(this);
            totalValueTextView.setText("+" + (-advantage));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            totalValueTextView.setLayoutParams(params);

            blackTakenPiecesLayout.addView(totalValueTextView);
        }
    }

    private int calculateTotalValue(List<Piece> takenPieces, Map<PieceType, Integer> pieceValueMap) {
        int totalValue = 0;
        for (Piece piece : takenPieces) {
            // Calculate the value for this piece type
            int pieceValue = pieceValueMap.getOrDefault(piece.getType(), 0);
            totalValue += pieceValue;
        }
        return totalValue;
    }

    private void displayPieces(List<Piece> takenPieces, LinearLayout takenPiecesLayout) {
        for (Piece piece : takenPieces) {
            // Create an ImageView for the piece
            ImageView pieceImageView = new ImageView(this);

            // Get the image resource ID for the piece
            int resourceId = getMicroPieceImageResource(piece);
            pieceImageView.setImageResource(resourceId);

            // Create a container LinearLayout to hold the ImageView
            LinearLayout pieceContainer = new LinearLayout(this);
            pieceContainer.setOrientation(LinearLayout.HORIZONTAL);
            pieceContainer.addView(pieceImageView);

            // Add the container to the main layout
            takenPiecesLayout.addView(pieceContainer);
        }
    }
    protected abstract void flipComponents();
}
