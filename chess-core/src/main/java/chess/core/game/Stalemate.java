package chess.core.game;

import chess.core.model.Position;

import java.util.Optional;

/**
 * Condición de fin de partida que detecta el rey ahogado.
 *
 * <p>Hay rey ahogado cuando al jugador al que le corresponde mover
 * no le queda ningún movimiento legal disponible, pero su rey
 * no está en jaque. En ese caso la partida termina en tablas.
 *
 * <p>La condición recibe un {@link GameContext} y utiliza únicamente
 * la posición actual y los movimientos legales. El historial de posiciones
 * forma parte del contexto para otras condiciones de fin de partida,
 * como las tablas por repetición, pero no es necesario para detectar
 * un rey ahogado.
 */
public final class Stalemate implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        Position position = context.position();

        if (context.legalMoves().isEmpty()
                && !position.isInCheck(position.sideToMove())) {

            return Optional.of(
                    GameResult.draw("Rey ahogado (tablas)")
            );
        }

        return Optional.empty();
    }
}