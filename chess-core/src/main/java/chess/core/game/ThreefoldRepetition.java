package chess.core.game;

import java.util.Optional;

/**
 * Condición de fin de partida que detecta tablas por triple repetición.
 *
 * <p>La partida termina en tablas cuando la posición actual ya se alcanzó
 * tres veces durante la partida (la posición inicial cuenta desde el primer
 * momento).
 *
 * <p>La igualdad de posiciones es la propia de {@code Position}: misma
 * geometría, mismas piezas, mismo turno y mismos derechos de enroque y
 * captura al paso. El reloj de medio movimientos no participa de la
 * comparación.
 */
public final class ThreefoldRepetition implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        long apariciones = context.positionHistory()
                .stream()
                .filter(position -> position.equals(context.position()))
                .count();

        if (apariciones >= 3) {
            return Optional.of(GameResult.draw("Triple repetición"));
        }

        return Optional.empty();
    }
}
