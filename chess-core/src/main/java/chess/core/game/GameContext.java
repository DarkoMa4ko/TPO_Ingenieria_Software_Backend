package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;
import java.util.Objects;

/**
 * Contexto evaluado por cada condición de fin de partida: agrupa la información
 * necesaria para decidir si la partida terminó, y evita que las condiciones
 * acoplen a {@link Game}.
 *
 * <p>Contiene la posición actual, los movimientos legales del jugador en turno
 * y el historial de posiciones alcanzadas. Cada condición utiliza únicamente
 * lo que necesita (por ejemplo, una condición basada en repetición consulta
 * el historial, mientras que jaque mate y ahogado no).
 */
public record GameContext(
        Position position,
        List<Move> legalMoves,
        List<Position> positionHistory
) {

    public GameContext {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(legalMoves, "legalMoves");
        Objects.requireNonNull(positionHistory, "positionHistory");
    }
}
