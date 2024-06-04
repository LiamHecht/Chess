package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class Pawn extends Piece {
    public Pawn(boolean isWhite) {
        super(isWhite, PieceType.PAWN);
    }
    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = destinationSpot.getX() - sourceSpot.getX();
        int deltaY = destinationSpot.getY() - sourceSpot.getY();

        // Check for regular moves
        if (deltaY == 0) {
            if (isWhite()) {
                // White pawns move forward  by one square
                if (deltaX == -1 && destinationSpot.getPiece() == null) {
                    return true;
                }

                // White pawns can move forward by two squares on their first move
                if (deltaX == -2 && sourceSpot.getX() == 6 && destinationSpot.getPiece() == null) {
                    return true;
                }
            } else {
                // Black pawns move forward by one square
                if (deltaX == 1 && destinationSpot.getPiece() == null) {
                    return true;
                }

                // Black pawns can move by two squares on their first move
                if (deltaX == 2 && sourceSpot.getX() == 1 && destinationSpot.getPiece() == null) {
                    return true;
                }
            }
        }

        // Pawns can capture diagonally
        if (Math.abs(deltaY) == 1) {
            // White pawn captures
            if (isWhite() && deltaX == -1 && destinationSpot.getPiece() != null && !destinationSpot.getPiece().isWhite()) {
                return true;
            }

            // Black pawn captures
            if (!isWhite() && deltaX == 1 && destinationSpot.getPiece() != null && destinationSpot.getPiece().isWhite()) {
                return true;
            }
        }

        return false; // Invalid move
    }
}
