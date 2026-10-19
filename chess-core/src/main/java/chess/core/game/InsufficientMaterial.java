package chess.core.game;

import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Position;
import chess.core.standard.StandardPieces;

import java.util.List;
import java.util.Optional;

/** Tablas automáticas cuando ninguno de los dos bandos puede dar mate. */
public final class InsufficientMaterial implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(Position position, List<Move> legalMoves) {
        if (!position.pieces(StandardPieces.PAWN).isEmpty()
                || !position.pieces(StandardPieces.ROOK).isEmpty()
                || !position.pieces(StandardPieces.QUEEN).isEmpty()) {
            return Optional.empty();
        }

        int whiteKnights = position.pieces(Color.WHITE, StandardPieces.KNIGHT).cardinality();
        int blackKnights = position.pieces(Color.BLACK, StandardPieces.KNIGHT).cardinality();
        int whiteBishops = position.pieces(Color.WHITE, StandardPieces.BISHOP).cardinality();
        int blackBishops = position.pieces(Color.BLACK, StandardPieces.BISHOP).cardinality();
        int minorPieces = whiteKnights + blackKnights + whiteBishops + blackBishops;

        // Rey contra rey, rey + un alfil contra rey, rey + un caballo contra rey.
        if (minorPieces <= 1) {
            return Optional.of(GameResult.draw("Material insuficiente"));
        }

        // Solo alfiles, todos situados en casillas del mismo color.
        if (whiteKnights == 0 && blackKnights == 0) {
            Boolean squareColor = null;
            for (var square : position.geometry().squares(position.pieces(StandardPieces.BISHOP))) {
                boolean color = ((square.file() + square.rank()) % 2 == 0);
                if (squareColor != null && squareColor != color) {
                    return Optional.empty();
                }
                squareColor = color;
            }
            return Optional.of(GameResult.draw("Material insuficiente"));
        }
        return Optional.empty();
    }
}
