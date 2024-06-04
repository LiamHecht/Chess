package com.example.chess.gameplay;

import com.example.chess.pieces.PieceType;

import java.io.Serializable;


public class Move implements Serializable {
    public int startX, startY;
    public int endX, endY;
    PieceType promotionType = null;

    public Move(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }
    @Override
    public String toString() {
        char fileStart = (char) ('a' + startY);
        int rankStart = 8 - startX;
        char fileEnd = (char) ('a' + endY);
        int rankEnd = 8 - endX;

        return "" + fileStart + rankStart + fileEnd + rankEnd;
    }
    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public PieceType getPromotionType() {
        return promotionType;
    }
    public void setPromotionType(PieceType promotionType) {
        this.promotionType = promotionType;
    }

}