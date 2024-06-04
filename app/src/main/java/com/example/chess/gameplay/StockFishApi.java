package com.example.chess.gameplay;

import android.util.Log;

import com.example.chess.pieces.Bishop;
import com.example.chess.pieces.King;
import com.example.chess.pieces.Knight;
import com.example.chess.pieces.Pawn;
import com.example.chess.pieces.Piece;
import com.example.chess.pieces.Queen;
import com.example.chess.pieces.Rook;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StockFishApi {
    private Board chessBoard;
    private int depth;
    private boolean isWhite;
    private int totalGameMoves;

    public StockFishApi(Board chessBoard, int depth, boolean isWhite) {
        this.chessBoard = chessBoard;
        this.depth = depth;
        this.isWhite = isWhite;
        if (isWhite) {
            totalGameMoves++;
        }
        Log.d("StockFishApi", "Initialized with depth: " + depth + " and isWhite: " + isWhite);
    }


        private String getFenSymbol(int x, int y) {
        Piece piece = chessBoard.getBox(x, y).getPiece();
        if (piece instanceof Pawn) {
            return piece.isWhite() ? "P" : "p"; // P for white pawn, p for black pawn
        } else if (piece instanceof Rook) {
            return piece.isWhite() ? "R" : "r"; // R for white rook, r for black rook
        } else if (piece instanceof Knight) {
            return piece.isWhite() ? "N" : "n"; // N for white knight, n for black knight
        } else if (piece instanceof Bishop) {
            return piece.isWhite() ? "B" : "b"; // B for white bishop, b for black bishop
        } else if (piece instanceof Queen) {
            return piece.isWhite() ? "Q" : "q"; // Q for white queen, q for black queen
        } else if (piece instanceof King) {
            return piece.isWhite() ? "K" : "k"; // K for white king, k for black king
        } else {
            // Count consecutive empty squares
            int emptyCount = 0;
            while (y < 8 && chessBoard.getBox(x, y).getPiece() == null) {
                emptyCount++;
                y++;
            }
            return String.valueOf(emptyCount);
        }
    }
    private String generateFEN() {
        StringBuilder fenBuilder = new StringBuilder();
        Log.d("StockFishApi", "Generating FEN for current board state.");

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = chessBoard.getBox(row, col).getPiece();
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount);
                        emptyCount = 0;
                    }
                    fenBuilder.append(getFenSymbol(row, col));
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount);
            }
            if (row < 7) {
                fenBuilder.append('/');
            }
        }
        fenBuilder.append(' ');
        fenBuilder.append(chessBoard.isWhiteTurn() ? 'w' : 'b');
        fenBuilder.append(" - - 0 1"); // Default values for castling, en passant, and move counters

        String fen = fenBuilder.toString();
        Log.d("StockFishApi", "Generated FEN: " + fen);
        return fen;
    }

    private String generateStockfishLink() {
        try {
            String fen = generateFEN();
            String encodedFen = URLEncoder.encode(fen, StandardCharsets.UTF_8.toString()); // URL encode the FEN
            String link = "https://stockfish.online/api/s/v2.php?fen=" + encodedFen + "&depth=" + depth;
            Log.d("StockFishApi", "Generated link for Stockfish API: " + link);
            return link;
        } catch (Exception e) {
            Log.e("StockFishApi", "Error encoding URL: " + e.getMessage());
            return null;
        }
    }

    public interface OnMoveReceivedListener {
        void onMoveReceived(String move);
        void onError(Exception e);
    }

    public void sendStockfishRequest(OnMoveReceivedListener listener) {
        new Thread(() -> {
            try {
                String stockfishLink = generateStockfishLink();
                Log.d("StockFishApi", "Sending request to Stockfish API.");
                URL url = new URL(stockfishLink);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                connection.disconnect();

                Log.d("StockFishApi", "Response from Stockfish API: " + response);
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getBoolean("success")) {
                    String bestMove = jsonResponse.getString("bestmove");
                    String[] splitMove = bestMove.split(" "); // Split by space
                    if (splitMove.length >= 2) {
                        listener.onMoveReceived(splitMove[1]); // Return the second part
                    } else {
                        throw new Exception("Invalid best move format: " + bestMove);
                    }
                } else {
                    throw new Exception("API call was not successful.");
                }
            } catch (Exception e) {
                Log.e("StockFishApi", "Error during Stockfish API request: " + e.getMessage());
                listener.onError(e);
            }
        }).start();
    }

}
