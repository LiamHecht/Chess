package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class King extends Piece {
    private boolean moved;

    public King(boolean isWhite) {
        super(isWhite, PieceType.KING);
        moved = false;
    }

    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int xDiff = Math.abs(sourceSpot.getX() - destinationSpot.getX());
        int yDiff = Math.abs(sourceSpot.getY() - destinationSpot.getY());

        // King can move one square in any direction
        return xDiff <= 1 && yDiff <= 1;
    }

    public boolean hasMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }
}
