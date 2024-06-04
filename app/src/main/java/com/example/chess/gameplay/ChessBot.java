package com.example.chess.gameplay;

import com.example.chess.pieces.Piece;

import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChessBot {
    private Board board;
    private int MAX_DEPTH;
    private boolean isWhite;
    private Random rand = new Random();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChessBot(Board board, int MAX_DEPTH, boolean isWhite) {
        this.board = board;
        this.MAX_DEPTH = MAX_DEPTH;
        this.isWhite = isWhite;
    }

    private Stack<Move> generateAllPossibleMovesForCurrentPlayer() {
        Stack<Move> possibleMoves = new Stack<>();
        boolean isWhiteTurn = board.isWhiteTurn();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot sourceSpot = board.getBox(i, j);
                Piece sourcePiece = sourceSpot.getPiece();
                if (sourcePiece != null && sourcePiece.isWhite() == isWhiteTurn) {
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            Spot destinationSpot = board.getBox(x, y);
                            if (board.isValidMove(sourceSpot, destinationSpot)) {
                                // Simulate the move and check if it results in a check
                                Piece originalDestPiece = destinationSpot.getPiece();
                                destinationSpot.setPiece(sourceSpot.getPiece());
                                sourceSpot.setPiece(null);
                                boolean kingInCheckAfterMove = board.isKingInCheck(isWhiteTurn);
                                sourceSpot.setPiece(destinationSpot.getPiece());
                                destinationSpot.setPiece(originalDestPiece);
                                if (!kingInCheckAfterMove) {
                                    possibleMoves.add(new Move(sourceSpot, destinationSpot));
                                }
                            }
                        }
                    }
                }
            }
        }
        return possibleMoves;
    }
    private void applyMove(Move move) {
        board.movePiece(move.source.getX(), move.source.getY(), move.destination.getX(), move.destination.getY());
    }

    private void undoMove(Move move) {
        Spot start = move.source;
        Spot end = move.destination;
        Piece movedPiece = end.getPiece();
        start.setPiece(movedPiece);
        end.setPiece(move.capturedPiece);
        board.toggleTurn();
    }

    private int evaluateBoard() {
        int score = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getBox(i, j).getPiece();
                if (piece != null) {
                    score += pieceValue(piece) * (piece.isWhite() ? 1 : -1);
                    score += positionBonus(piece, i, j);
                }
            }
        }
        return score;
    }

    private int positionBonus(Piece piece, int x, int y) {
        // Central control bonus. You can expand this with more detailed tables.
        if ((x > 2 && x < 5) && (y > 2 && y < 5)) {
            return (piece.isWhite() ? 1 : -1) * 5;
        }
        return 0;
    }

    private int pieceValue(Piece piece) {
        switch (piece.getType()) {
            case PAWN:
                return 10;
            case KNIGHT:
                return 30;
            case BISHOP:
                return 30;
            case ROOK:
                return 50;
            case QUEEN:
                return 90;
            case KING:
                return 1000;
            default:
                return 0;
        }
    }
    public void shutdown() {
        executor.shutdown();
    }
    public Move getBestMove() {
        return minimax(MAX_DEPTH, board.isWhiteTurn(), Integer.MIN_VALUE, Integer.MAX_VALUE).move;
    }

    private EvaluatedMove minimax(int depth, boolean isWhite, int alpha, int beta) {
        Stack<Move> possibleMoves = generateAllPossibleMovesForCurrentPlayer();

        if (possibleMoves.isEmpty() || depth == 0) {
            return new EvaluatedMove(null, evaluateBoard());
        }

        Move bestMove = null;

        if (isWhite) {
            int bestScore = Integer.MIN_VALUE;
            while (!possibleMoves.isEmpty()) {
                Move currentMove = possibleMoves.pop();
                applyMove(currentMove);
                int currentScore = minimax(depth - 1, isWhite, alpha, beta).score;
                undoMove(currentMove);

                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestMove = currentMove;
                }

                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return new EvaluatedMove(bestMove, bestScore);
        } else {
            int bestScore = Integer.MAX_VALUE;
            while (!possibleMoves.isEmpty()) {
                Move currentMove = possibleMoves.pop();
                applyMove(currentMove);
                int currentScore = minimax(depth - 1, isWhite, alpha, beta).score;
                undoMove(currentMove);

                if (currentScore < bestScore) {
                    bestScore = currentScore;
                    bestMove = currentMove;
                }

                beta = Math.min(beta, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return new EvaluatedMove(bestMove, bestScore);
        }
    }

    private class EvaluatedMove {
        public Move move;
        public int score;

        public EvaluatedMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
    public class Move {
        public Spot source;
        public Spot destination;
        public Piece capturedPiece;

        public Move(Spot source, Spot destination) {
            this.source = source;
            this.destination = destination;
            this.capturedPiece = destination.getPiece();
        }
    }

}

