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
        Stack<Move> possibleMoves = new Stack<>(); // Stack to store all possible moves
        boolean isWhiteTurn = board.isWhiteTurn(); // Determine if it is white's turn

        // Iterate over all spots on the chessboard to find pieces belonging to the current player
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Spot sourceSpot = board.getBox(i, j); // Get the current spot
                Piece sourcePiece = sourceSpot.getPiece(); // Get the piece at the current spot

                // Check if the piece belongs to the current player
                if (sourcePiece != null && sourcePiece.isWhite() == isWhiteTurn) {
                    // Iterate over all possible destination spots
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            Spot destinationSpot = board.getBox(x, y); // Get the destination spot

                            // Check if the move from source to destination is valid
                            if (board.isValidMove(sourceSpot, destinationSpot)) {
                                // Simulate the move and check if it results in a check
                                Piece originalDestPiece = destinationSpot.getPiece(); // Save the original destination piece
                                destinationSpot.setPiece(sourceSpot.getPiece()); // Move the piece to the destination spot
                                sourceSpot.setPiece(null); // Clear the source spot

                                // Check if the king is in check after the move
                                boolean kingInCheckAfterMove = board.isKingInCheck(isWhiteTurn);

                                // Undo the move
                                sourceSpot.setPiece(destinationSpot.getPiece()); // Move the piece back to the source spot
                                destinationSpot.setPiece(originalDestPiece); // Restore the original destination piece

                                // If the move does not result in a check, add it to the possible moves stack
                                if (!kingInCheckAfterMove) {
                                    possibleMoves.add(new Move(sourceSpot, destinationSpot));
                                }
                            }
                        }
                    }
                }
            }
        }
        return possibleMoves; // Return the stack of all possible moves
    }

    private void applyMove(Move move) {
        // Apply the move by moving the piece from the source spot to the destination spot on the board
        board.movePiece(move.source.getX(), move.source.getY(), move.destination.getX(), move.destination.getY());
    }

    private void undoMove(Move move) {
        Spot start = move.source; // Get the source spot of the move
        Spot end = move.destination; // Get the destination spot of the move
        Piece movedPiece = end.getPiece(); // Get the piece that was moved to the destination spot

        // Move the piece back to the source spot
        start.setPiece(movedPiece);

        // Restore the captured piece (if any) to the destination spot
        end.setPiece(move.capturedPiece);

        // Toggle the turn back to the original player
        board.toggleTurn();
    }

    private int evaluateBoard() {
        int score = 0; // Initialize the score

        // Iterate over all spots on the chessboard to evaluate the board state
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getBox(i, j).getPiece(); // Get the piece at the current spot

                // If there is a piece at the current spot, evaluate its value and position
                if (piece != null) {
                    // Add the piece's value to the score (positive for white, negative for black)
                    score += pieceValue(piece) * (piece.isWhite() ? 1 : -1);

                    // Add the positional bonus for the piece
                    score += positionBonus(piece, i, j);
                }
            }
        }
        return score; // Return the evaluated score of the board
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
        Stack<Move> possibleMoves = generateAllPossibleMovesForCurrentPlayer(); // Generate all possible moves for the current player

        // Base case: if no more moves are possible or the depth is 0, evaluate the board
        if (possibleMoves.isEmpty() || depth == 0) {
            return new EvaluatedMove(null, evaluateBoard()); // Return the board evaluation
        }

        Move bestMove = null; // Initialize the best move

        if (isWhite) {
            int bestScore = Integer.MIN_VALUE; // Initialize the best score for maximizing player
            while (!possibleMoves.isEmpty()) {
                Move currentMove = possibleMoves.pop(); // Get the next move
                applyMove(currentMove); // Apply the move

                // Recursively call minimax with decreased depth and toggle the player
                int currentScore = minimax(depth - 1, !isWhite, alpha, beta).score;
                undoMove(currentMove); // Undo the move

                // Update the best score and best move if the current score is better
                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestMove = currentMove;
                }

                // Alpha-beta pruning
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) {
                    break; // Beta cut-off
                }
            }
            return new EvaluatedMove(bestMove, bestScore); // Return the best move and score for the maximizing player
        } else {
            int bestScore = Integer.MAX_VALUE; // Initialize the best score for minimizing player
            while (!possibleMoves.isEmpty()) {
                Move currentMove = possibleMoves.pop(); // Get the next move
                applyMove(currentMove); // Apply the move

                // Recursively call minimax with decreased depth and toggle the player
                int currentScore = minimax(depth - 1, !isWhite, alpha, beta).score;
                undoMove(currentMove); // Undo the move

                // Update the best score and best move if the current score is better
                if (currentScore < bestScore) {
                    bestScore = currentScore;
                    bestMove = currentMove;
                }

                // Alpha-beta pruning
                beta = Math.min(beta, bestScore);
                if (beta <= alpha) {
                    break; // Alpha cut-off
                }
            }
            return new EvaluatedMove(bestMove, bestScore); // Return the best move and score for the minimizing player
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

