package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class Rook extends Piece {
    private boolean moved;

    public Rook(boolean isWhite) {
        super(isWhite, PieceType.ROOK);
        moved = false;
    }

    public boolean hasMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = Math.abs(destinationSpot.getX() - sourceSpot.getX());
        int deltaY = Math.abs(destinationSpot.getY() - sourceSpot.getY());

        // Rook can move horizontally or vertically
        return (deltaX > 0 && deltaY == 0) || (deltaY > 0 && deltaX == 0);
    }
}
