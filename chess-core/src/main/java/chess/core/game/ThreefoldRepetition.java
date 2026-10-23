package chess.core.game;

import chess.core.model.Position;

import java.util.Optional;

/**
 * Condición de fin de partida que detecta tablas por triple repetición.
 *
 * <p>La condición compara la posición actual con las posiciones alcanzadas
 * anteriormente durante la partida. Si la misma posición aparece al menos
 * tres veces en el historial, la partida termina en tablas.
 *
 * <p>La comparación se realiza utilizando la igualdad definida por
 * {@link Position}, de modo que la condición trabaja únicamente con
 * conceptos del dominio y no conoce detalles de persistencia, base de datos,
 * API o interfaz de usuario.
 */
public final class ThreefoldRepetition implements EndCondition {

    private static final long REQUIRED_OCCURRENCES = 3;

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        Position current = context.position();

        long occurrences = context.positionHistory()
                .stream()
                .filter(current::equals)
                .count();

        if (occurrences >= REQUIRED_OCCURRENCES) {
            return Optional.of(
                    GameResult.draw("Triple repetición")
            );
        }

        return Optional.empty();
    }
}