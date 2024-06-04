package com.example.chess.gameplay;

import java.util.ArrayList;
import java.util.List;

public class ChessPgn {
    private List<String> pgnMoves = new ArrayList<>();

    public void addMove(String move) {
        this.pgnMoves.add(move);
    }
    public void setPgnMoves(List<String> pgnMoves) {
        this.pgnMoves = pgnMoves;
    }
    public List<String> getPgnMoves() {
        return pgnMoves;
    }
    public int getLength(){
        return pgnMoves.size();
    }
}