package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;
import java.util.Optional;

/** Jaque mate: al jugador en turno no le quedan movimientos legales y su rey está en jaque. */
public final class Checkmate implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(Position position, List<Move> legalMoves) {
        if (legalMoves.isEmpty() && position.isInCheck(position.sideToMove())) {
            return Optional.of(GameResult.win(position.sideToMove().opposite(), "Jaque mate"));
        }
        return Optional.empty();
    }
}
