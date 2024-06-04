package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class Bishop extends Piece {
    public Bishop(boolean isWhite) {
        super(isWhite, PieceType.BISHOP);
    }

    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = Math.abs(destinationSpot.getX() - sourceSpot.getX());
        int deltaY = Math.abs(destinationSpot.getY() - sourceSpot.getY());

        // Bishop can move diagonally
        return deltaX == deltaY;
    }
}
