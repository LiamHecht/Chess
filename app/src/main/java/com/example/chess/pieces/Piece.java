package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

// Base class for all chess pieces
public abstract class Piece {
    private boolean isWhite;
    private PieceType type;

    public Piece(boolean isWhite, PieceType type) {
        this.isWhite = isWhite;
        this.type = type;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public PieceType getType() {
        return type;
    }

    public abstract boolean isValidMove(Spot sourceSpot, Spot destinationSpot);

    public String getName() {
        switch (this.type) {
            case ROOK:
                return "Rook";
            case KNIGHT:
                return "Knight";
            case BISHOP:
                return "Bishop";
            case KING:
                return "King";
            case QUEEN:
                return "Queen";
            case PAWN:
                return "Pawn";
            default:
                return "Unknown";
        }
    }

}












