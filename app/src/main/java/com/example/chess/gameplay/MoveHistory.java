package com.example.chess.gameplay;

import com.example.chess.pieces.Piece;
import com.example.chess.pieces.PieceType;

public class MoveHistory {
    private final Move move;
    private final Piece capturedPiece;
    private SpecialMoveType specialMoveType;
    private int capturedPieceX;
    private int capturedPieceY;
    private PieceType promotedType;
    public MoveHistory(Move move, Piece capturedPiece) {
        this.move = move;
        this.capturedPiece = capturedPiece;
    }

    public Move getMove() {
        return move;
    }

    @Override
    public String toString() {
        return "MoveHistory{" +
                "move=" + move +
                ", capturedPiece=" + capturedPiece +
                ", specialMoveType=" + specialMoveType +
                ", capturedPieceX=" + capturedPieceX +
                ", capturedPieceY=" + capturedPieceY +
                ", promotedType=" + promotedType +
                '}';
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public Move getReversedMove() {
        return new Move(move.getEndX(), move.getEndY(), move.getStartX(), move.getStartY());
    }

    public SpecialMoveType getSpecialMoveType() {
        return specialMoveType;
    }

    public void setSpecialMoveType(SpecialMoveType specialMoveType) {
        this.specialMoveType = specialMoveType;
    }

    public int getCapturedPieceX() {
        return capturedPieceX;
    }

    public void setCapturedPieceX(int capturedPieceX) {
        this.capturedPieceX = capturedPieceX;
    }

    public int getCapturedPieceY() {
        return capturedPieceY;
    }

    public void setCapturedPieceY(int capturedPieceY) {
        this.capturedPieceY = capturedPieceY;
    }
}
