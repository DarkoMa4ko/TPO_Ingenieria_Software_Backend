package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;

public record GameContext(
        Position position,
        List<Move> legalMoves,
        List<Position> positionHistory
) {

    public GameContext {
        legalMoves = List.copyOf(legalMoves);
        positionHistory = List.copyOf(positionHistory);
    }
}