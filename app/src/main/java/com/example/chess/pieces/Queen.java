package com.example.chess.pieces;

import com.example.chess.gameplay.Spot;

public class Queen extends Piece {
    public Queen(boolean isWhite) {
        super(isWhite, PieceType.QUEEN);
    }
    @Override
    public boolean isValidMove(Spot sourceSpot, Spot destinationSpot) {
        int deltaX = Math.abs(destinationSpot.getX() - sourceSpot.getX());
        int deltaY = Math.abs(destinationSpot.getY() - sourceSpot.getY());

        // Queen can move horizontally, vertically, or diagonally
        return (deltaX > 0 && deltaY == 0) || (deltaY > 0 && deltaX == 0) || (deltaX == deltaY);
    }

}
