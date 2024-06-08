package com.example.chess.gameplay;

import android.util.Pair;

import com.example.chess.pieces.Bishop;
import com.example.chess.pieces.King;
import com.example.chess.pieces.Knight;
import com.example.chess.pieces.Pawn;
import com.example.chess.pieces.Piece;
import com.example.chess.pieces.PieceType;
import com.example.chess.pieces.Queen;
import com.example.chess.pieces.Rook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private Spot[][] boxes;
    private boolean isWhite = true;
    private Spot lastMoveStart = null;
    private Spot lastMoveEnd = null;
    private List<String> pastPositions = new ArrayList<>();

    private boolean isGameOver = false;
    public Board() {
        this.boxes = new Spot[8][8];
        this.resetBoard();
    }

    public Spot getLastMoveStart() {
        return lastMoveStart;
    }

    public void setLastMoveStart(Spot spot) {
        this.lastMoveStart = spot;
    }

    public Spot getLastMoveEnd() {
        return lastMoveEnd;
    }

    public void setLastMoveEnd(Spot spot) {
        this.lastMoveEnd = spot;
    }

    public boolean isWhiteTurn(){
        return isWhite;
    }
    public Spot getBox(int x, int y) throws IndexOutOfBoundsException {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IndexOutOfBoundsException("Index out of bound");
        }

        return boxes[x][y];
    }
    public void setBox(int x, int y, Piece piece) throws IndexOutOfBoundsException {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IndexOutOfBoundsException("Index out of bound");
        }
        boxes[x][y].setPiece(piece);
    }


    private void resetBoard() {
        // Initialize black pieces
        boxes[0][0] = new Spot(0, 0, new Rook(false));
        boxes[0][1] = new Spot(0, 1, new Knight(false));
        boxes[0][2] = new Spot(0, 2, new Bishop(false));
        boxes[0][3] = new Spot(0, 3, new Queen(false));
        boxes[0][4] = new Spot(0, 4, new King(false));
        boxes[0][5] = new Spot(0, 5, new Bishop(false));
        boxes[0][6] = new Spot(0, 6, new Knight(false));
        boxes[0][7] = new Spot(0, 7, new Rook(false));

        for (int i = 0; i < 8; i++) {
            boxes[1][i] = new Spot(1, i, new Pawn(false));
        }

//         Initialize white pieces
        boxes[7][0] = new Spot(7, 0, new Rook(true));
        boxes[7][1] = new Spot(7, 1, new Knight(true));
        boxes[7][2] = new Spot(7, 2, new Bishop(true));
        boxes[7][3] = new Spot(7, 3, new Queen(true));
        boxes[7][4] = new Spot(7, 4, new King(true));
        boxes[7][5] = new Spot(7, 5, new Bishop(true));
        boxes[7][6] = new Spot(7, 6, new Knight(true));
        boxes[7][7] = new Spot(7, 7, new Rook(true));

        for (int i = 0; i < 8; i++) {
            boxes[6][i] = new Spot(6, i, new Pawn(true));
        }

        // Initialize remaining boxes without any piece
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                boxes[i][j] = new Spot(i, j, null);
            }
        }
    }

    public void promotePawn(int x, int y, PieceType promoteTo) {
        Spot spot = getBox(x, y);
        Piece piece = spot.getPiece();

        if (piece != null && piece.getType() == PieceType.PAWN) {
            switch (promoteTo) {
                case QUEEN:
                    spot.setPiece(new Queen(piece.isWhite()));
                    break;
                case ROOK:
                    spot.setPiece(new Rook(piece.isWhite()));
                    break;
                case BISHOP:
                    spot.setPiece(new Bishop(piece.isWhite()));
                    break;
                case KNIGHT:
                    spot.setPiece(new Knight(piece.isWhite()));
                    break;
                default:
                    break;
            }
        }
    }

    public boolean movePiece(int x, int y, int newX, int newY) {
        // Check if the source and destination positions are valid
        if (x < 0 || x > 7 || y < 0 || y > 7 || newX < 0 || newX > 7 || newY < 0 || newY > 7) {
            throw new IllegalArgumentException("Invalid position");
        }

        Spot sourceSpot = boxes[x][y];
        Spot destinationSpot = boxes[newX][newY];

        // Check if there is a piece at the source position
        if (sourceSpot.getPiece() == null) {
            return false;
        }

        // Check if the move is valid for the specific piece
        if (!isValidMove(sourceSpot, destinationSpot)) {
            return false; // Invalid move
        }

        Piece capturedPiece = destinationSpot.getPiece();
        destinationSpot.setPiece(sourceSpot.getPiece());
        sourceSpot.setPiece(null);
        Piece piece = destinationSpot.getPiece();
        // Set the last moves
        setLastMoveStart(sourceSpot);
        setLastMoveEnd(destinationSpot);

        isWhite = !isWhite; // Update the current player

        // If it's a non-pawn, non-capturing move, then record the position.
        if (!(piece instanceof Pawn) && capturedPiece == null) {
            pastPositions.add(getBoardHash());
        } else {
            pastPositions.clear(); // Clear the list
        }
        if (piece instanceof King){
            ((King) piece).setMoved(true);
        }
        if (piece instanceof Rook){
            ((Rook) piece).setMoved(true);
        }
        return true;
    }

    public void placePieceWithoutValidation(int x, int y, int newX, int newY) {
        // Check if the source and destination positions are valid
        if (x < 0 || x > 7 || y < 0 || y > 7 || newX < 0 || newX > 7 || newY < 0 || newY > 7) {
            throw new IllegalArgumentException("Invalid position");
        }

        Spot sourceSpot = boxes[x][y];
        Spot destinationSpot = boxes[newX][newY];

        // Simply move the piece without validation
        destinationSpot.setPiece(sourceSpot.getPiece());
        sourceSpot.setPiece(null);

    }

    public void toggleTurn() {
        isWhite = !isWhite;
    }

    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        Piece piece = sourceSpot.getPiece();

        // Check if the destination spot is occupied by a piece of the same color
        if (destinationSpot.getPiece() != null && piece.isWhite() == destinationSpot.getPiece().isWhite()) {
            return false;
        }

        // Delegate move validation to the specific piece class
        if (!piece.isValidMove(sourceSpot, destinationSpot)) {
            return false;
        }

        // Check if the path is clear for the move
        return isPathClear(sourceSpot, destinationSpot);
    }
    public Pair<Boolean, Boolean> simulateMove(int startX, int startY, int endX, int endY) {
        Spot startSpot = boxes[startX][startY];
        Spot endSpot = boxes[endX][endY];

        Piece movingPiece = startSpot.getPiece();
        if (movingPiece == null) {
            return new Pair<>(false, false);
        }

        Piece capturedPiece = endSpot.getPiece();

        // Temporarily make the move
        endSpot.setPiece(movingPiece);
        startSpot.setPiece(null);

        boolean ownKingInCheck = isKingInCheck(movingPiece.isWhite());
        boolean opponentKingInCheck = isKingInCheck(!movingPiece.isWhite());

        // Rollback the move
        startSpot.setPiece(movingPiece);
        endSpot.setPiece(capturedPiece);

        return new Pair<>(!ownKingInCheck, opponentKingInCheck);
    }
    public boolean isGameOver(){
        return isGameOver;
    }

    public boolean isCheckmate(boolean isWhite) {
        if (!isKingInCheck(isWhite)) {
            return false;
        }

        // check if there is any legal move
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot sourceSpot = boxes[i][j];
                Piece sourcePiece = sourceSpot.getPiece();

                if (sourcePiece != null && sourcePiece.isWhite() == isWhite) {
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            Spot destinationSpot = boxes[x][y];

                            if (isValidMove(sourceSpot, destinationSpot)) {
                                Pair<Boolean, Boolean> simulationResults = simulateMove(i, j, x, y);

                                // Check if the move doesnt expose the king to check
                                if (simulationResults.first) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean isDraw() {
        if (isStalemate()) {
            return true;
        }

        if (insufficientMaterial()) {
            return true;
        }

        if (isThreefoldRepetition()) {
            return true;
        }

        return false;
    }
    private boolean isStalemate() {
        // If it's not the player's King in check, see if they have any legal moves left
        if (!isKingInCheck(isWhite)) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Spot startSpot = boxes[i][j];
                    Piece piece = startSpot.getPiece();
                    if (piece != null && piece.isWhite() == isWhite) {
                        for (int x = 0; x < 8; x++) {
                            for (int y = 0; y < 8; y++) {
                                Spot endSpot = boxes[x][y];
                                if (isValidMove(startSpot, endSpot)) {

                                    return false;  // Player has a legal move
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
    private boolean insufficientMaterial() {
        List<Piece> pieces = new ArrayList<>();

        // Collect all pieces on the board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (boxes[i][j].getPiece() != null) {
                    pieces.add(boxes[i][j].getPiece());
                }
            }
        }

        // Only king remains for both sides
        if (pieces.size() == 2) return true;

        // King and bishop/knight vs king
        if (pieces.size() == 3) {
            for (Piece piece : pieces) {
                if (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.KNIGHT) {
                    return true;
                }
            }
        }

        return false;
    }
    private boolean isThreefoldRepetition() {
        if (pastPositions.size() < 3) return false;

        String currentHash = getBoardHash();
        int occurrences = 0;

        for (String hash : pastPositions) {
            if (hash.equals(currentHash)) {
                occurrences++;
            }
        }

        return occurrences >= 3;
    }
    private String getBoardHash() {
        StringBuilder hash = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = boxes[i][j].getPiece();
                if (piece != null) {
                    hash.append(piece.isWhite() ? "W" : "B");
                    hash.append(piece.getType().toString());
                } else {
                    hash.append("00");
                }
            }
        }

        // Add the turn to the hash to differentiate between different players' moves
        hash.append(isWhite ? "W" : "B");

        return hash.toString();
    }

    public boolean isKingInCheck(boolean isWhite) {
        // Find the spot of the king of the specified color
        Spot kingSpot = findKing(isWhite);

        // Iterate over all spots on the chessboard
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot spot = boxes[i][j];
                // Check if the spot has an opponent's piece
                if (spot.getPiece() != null && spot.getPiece().isWhite() != isWhite) {
                    // Check if this opponent's piece can move to the king's spot
                    if (isValidMove(spot, kingSpot)) {
                        return true; // The king is in check
                    }
                }
            }
        }
        return false; // The king is not in check
    }

    public Spot findKing(boolean isWhite) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot spot = boxes[i][j];
                Piece piece = spot.getPiece();
                if (piece != null && piece.getType() == PieceType.KING && piece.isWhite() == isWhite) {
                    return spot;
                }
            }
        }
        return null;// Return null if no king is found (should not happen in a valid game)
    }
    private boolean isPathClear(Spot sourceSpot, Spot destinationSpot) {
        PieceType pieceType = sourceSpot.getPiece().getType();
        int deltaX = Math.abs(destinationSpot.getX() - sourceSpot.getX());
        int deltaY = Math.abs(destinationSpot.getY() - sourceSpot.getY());

        switch (pieceType) {
            case PAWN:
                // Special case for pawn's double move
                if (deltaX == 0 && deltaY == 2) { // Double move
                    return isPathClearPawnDoubleMove(sourceSpot, destinationSpot);
                }
            case ROOK:
                // Rook moves horizontally or vertically
                if ((deltaX > 0 && deltaY == 0) || (deltaY > 0 && deltaX == 0)) {
                    return isPathClearHorizontal(sourceSpot, destinationSpot) || isPathClearVertical(sourceSpot, destinationSpot);
                }
            case BISHOP:
                // Bishop moves diagonally
                if (deltaX == deltaY) {
                    return isPathClearDiagonal(sourceSpot, destinationSpot);
                }
                break;

            case QUEEN:
                // Queen moves horizontally, vertically, or diagonally
                if ((deltaX > 0 && deltaY == 0) || (deltaY > 0 && deltaX == 0)) {
                    return isPathClearHorizontal(sourceSpot, destinationSpot) || isPathClearVertical(sourceSpot, destinationSpot);
                } else if (deltaX == deltaY) {
                    return isPathClearDiagonal(sourceSpot, destinationSpot);
                }
                break;

            case KNIGHT:
                // Knight's path is always clear since it jumps over pieces
                return true;

            default:
                break;
        }

        return true;
    }
    private boolean isPathClearPawnDoubleMove(Spot sourceSpot, Spot destinationSpot) {
        int startX = sourceSpot.getX();
        int startY = sourceSpot.getY();
        int endX = destinationSpot.getX();

        // Determine the direction of the move
        int step = (endX > startX) ? 1 : -1;

        // Check the intermediate spot
        int intermediateX = startX + step;
        if (boxes[intermediateX][startY].getPiece() != null) {
            return false;
        }

        return true;
    }
    private boolean isPathClearDiagonal(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = destinationSpot.getX() - sourceSpot.getX();
        int deltaY = destinationSpot.getY() - sourceSpot.getY();

        int rowStep = (deltaX > 0) ? 1 : -1;
        int colStep = (deltaY > 0) ? 1 : -1;

        int row = sourceSpot.getX() + rowStep;
        int col = sourceSpot.getY() + colStep;

        while (row != destinationSpot.getX() || col != destinationSpot.getY()) {
            Spot spot = boxes[row][col];
            if (spot.getPiece() != null) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }

        return true;
    }


    private boolean isPathClearHorizontal(Spot sourceSpot, Spot destinationSpot) {
        int startX = sourceSpot.getX();
        int startY = sourceSpot.getY();
        int endY = destinationSpot.getY();

        int step = (endY > startY) ? 1 : -1;
        int col = startY + step;

        while (col != endY) {
            if (col < 0 || col >= boxes[0].length) {
                return false;
            }

            Spot spot = boxes[startX][col];
            if (spot.getPiece() != null) {
                return false;
            }
            col += step;
        }

        return true;
    }

    private boolean isPathClearVertical(Spot sourceSpot, Spot destinationSpot) {
        int startX = sourceSpot.getX();
        int startY = sourceSpot.getY();
        int endX = destinationSpot.getX();

        int step = (endX > startX) ? 1 : -1;
        int row = startX + step;

        while (row != endX) {
            if (row < 0 || row >= boxes.length) {
                return false; // Out of bounds
            }

            Spot spot = boxes[row][startY];
            if (spot.getPiece() != null) {
                return false;
            }
            row += step;
        }

        return true;
    }
    public boolean isCastlingMove(int startX, int startY, int endX, int endY) {
        Piece selectedPiece = boxes[startX][startY].getPiece();
        if (selectedPiece instanceof King && Math.abs(startY -endY) == 2 && !((King) selectedPiece).hasMoved()) {
            int rookColumn = (endY > startY) ? 7 : 0;
            Piece potentialRook = boxes[endX][rookColumn].getPiece();
            return potentialRook instanceof Rook && !((Rook) potentialRook).hasMoved();
        }
        return false;
    }
    private boolean canCastle(int kingX, int kingY, boolean isKingSide) {
        Piece kingPiece = boxes[kingX][kingY].getPiece();
        if (!(kingPiece instanceof King)) return false;
        King king = (King) kingPiece;
        boolean isKingWhite = king.isWhite();

        // Check if the king has moved
        if (king.hasMoved()) return false;

        // Determine the rook's position based on kingside or queenside castling
        int rookX = kingX;
        int rookY = isKingSide ? 7 : 0;

        Spot rookSpot = boxes[rookX][rookY];

        // Check if there's a rook in the rook's spot
        if (!(rookSpot.getPiece() instanceof Rook)) return false;
        Rook rook = (Rook) rookSpot.getPiece();

        // Check if the rook has moved
        if (rook.hasMoved()) return false;

        // Check if there are no pieces between the king and the rook
        int startY = Math.min(kingY, rookY);
        int endY = Math.max(kingY, rookY);
        for (int y = startY + 1; y < endY; y++) {
            if (boxes[rookX][y].getPiece() != null) return false;
            // check if there squeres arent Threatened
            if (isSquereThreatened(rookX, y, isKingWhite) && y!=1) return false;
        }

        return true;
    }
    private boolean isSquereThreatened(int x, int y, Boolean isWhiteCastle){
        Spot destSpot = boxes[x][y];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot spot = boxes[i][j];
                if (spot.getPiece() != null && spot.getPiece().isWhite() != isWhiteCastle) {
                    if(isValidMove(spot, destSpot)) return true;
                }
            }
        }
        return false;
    }
    public boolean performCastling(int kingX, int kingY, boolean isKingSide) {
        // Check if castling is allowed
        if (!canCastle(kingX, kingY, isKingSide)) {
            return false; // Castling not allowed
        }

        // Determine the final positions for the king and rook based on the castling side
        int kingFinalY = isKingSide ? 6 : 2; // King moves two squares to the right for king-side, or to the left for queen-side
        int rookFinalY = isKingSide ? 5 : 3; // Rook moves next to the king

        // Get the king piece from its initial position
        Piece kingPiece = boxes[kingX][kingY].getPiece();
        if (!(kingPiece instanceof King)) return false; // Ensure the piece is indeed a king
        King king = (King) kingPiece;

        // Move the king to its final position
        boxes[kingX][kingFinalY].setPiece(king);
        boxes[kingX][kingY].setPiece(null); // Clear the king's initial position
        king.setMoved(true); // Mark the king as having moved

        // Get the rook piece from its initial position
        Rook rook = (Rook) boxes[kingX][isKingSide ? 7 : 0].getPiece();
        // Move the rook to its final position
        boxes[kingX][rookFinalY].setPiece(rook);
        boxes[kingX][isKingSide ? 7 : 0].setPiece(null); // Clear the rook's initial position
        rook.setMoved(true); // Mark the rook as having moved

        // Switch the turn to the other player
        isWhite = !isWhite;
        return true; // Castling performed successfully
    }
    public boolean isEnPassant(int startX, int startY, int endX, int endY) {
        Spot movingPawnSpot = boxes[startX][startY];
        Spot endSpot = boxes[endX][endY];
        Spot adjacentPawnSpot;

        // Ensure that the pawn that's trying to capture is indeed a pawn.
        if (!(movingPawnSpot.getPiece() instanceof Pawn)) {
            return false;
        }

        // Check if the pawn is trying to move diagonally to an empty square
        if (Math.abs(startY - endY) != 1 || endSpot.getPiece() != null) {
            return false;
        }

        // Depending on the color of the moving pawn, find the adjacent pawn's spot.
        if (movingPawnSpot.getPiece().isWhite()) {
            // For white pawns, they should be moving 'up' the board,  decreasing x value
            if (endX != startX - 1) {
                return false;
            }
            adjacentPawnSpot = boxes[endX + 1][endY];
        } else {
            // For black pawns, they should be moving 'down' the board, increasing x value
            if (endX != startX + 1) {
                return false;
            }
            adjacentPawnSpot = boxes[endX - 1][endY];
        }

        // Check that the last move was a two-square pawn advance.
        if (!(lastMoveEnd.getPiece() instanceof Pawn)) {
            return false;
        }

        int lastMoveStartX = lastMoveStart.getX();
        int lastMoveEndX = lastMoveEnd.getX();

        if (Math.abs(lastMoveStartX - lastMoveEndX) != 2) {
            return false;
        }

        // Ensure the captured pawn is the same one that moved two squares in the last move.
        if (adjacentPawnSpot != lastMoveEnd) {
            return false;
        }

        return true;
    }

    public boolean performEnPassant(int startX, int startY, int endX, int endY) {
        Spot movingPawnSpot = getBox(startX, startY);
        Spot endSpot = getBox(endX, endY);
        Spot capturedPawnSpot;

        // Depending on the color of the moving pawn, find the spot of the captured pawn.
        if (movingPawnSpot.getPiece().isWhite()) {
            capturedPawnSpot = getBox(endX + 1, endY);
        } else {
            capturedPawnSpot = getBox(endX - 1, endY);
        }

        int lastMoveStartY = lastMoveStart.getY();
        int lastMoveEndY = lastMoveEnd.getY();

        // perform the en passant capture.
        endSpot.setPiece(movingPawnSpot.getPiece());   // The capturing pawn moves to the destination square.
        movingPawnSpot.setPiece(null);                 // The original square of the capturing pawn is now empty.
        capturedPawnSpot.setPiece(null);               // The captured pawn is removed.

        isWhite = !isWhite; // Update the current player

        return true;
    }



}