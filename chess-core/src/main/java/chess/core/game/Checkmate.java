package chess.core.game;

import chess.core.model.Position;

import java.util.Optional;

/**
 * Condición de fin de partida que detecta el jaque mate.
 *
 * <p>Hay jaque mate cuando el jugador al que le corresponde mover
 * no tiene ningún movimiento legal disponible y su rey está en jaque.
 *
 * <p>La condición recibe un {@link GameContext} y utiliza únicamente
 * la posición actual y los movimientos legales. El historial de posiciones
 * forma parte del contexto para otras condiciones de fin de partida,
 * como las tablas por repetición, pero no es necesario para detectar
 * el jaque mate.
 */
public final class Checkmate implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        Position position = context.position();

        if (context.legalMoves().isEmpty()
                && position.isInCheck(position.sideToMove())) {

            return Optional.of(
                    GameResult.win(
                            position.sideToMove().opposite(),
                            "Jaque mate"
                    )
            );
        }

        return Optional.empty();
    }
}