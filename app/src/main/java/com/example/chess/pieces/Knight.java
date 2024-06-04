package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class Knight extends Piece {
    public Knight(boolean isWhite) {
        super(isWhite, PieceType.KNIGHT);
    }
    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = Math.abs(destinationSpot.getX() - sourceSpot.getX());
        int deltaY = Math.abs(destinationSpot.getY() - sourceSpot.getY());

        // Knight can move in an L-shape
        return (deltaX == 1 && deltaY == 2) || (deltaX == 2 && deltaY == 1);
    }

}

