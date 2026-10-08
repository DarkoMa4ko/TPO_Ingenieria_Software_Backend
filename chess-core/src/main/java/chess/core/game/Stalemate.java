package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;
import java.util.Optional;

/** Rey ahogado: al jugador en turno no le quedan movimientos legales, pero su rey NO está en jaque. */
public final class Stalemate implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(Position position, List<Move> legalMoves) {
        if (legalMoves.isEmpty() && !position.isInCheck(position.sideToMove())) {
            return Optional.of(GameResult.draw("Rey ahogado (tablas)"));
        }
        return Optional.empty();
    }
}
