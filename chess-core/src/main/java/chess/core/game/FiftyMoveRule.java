package chess.core.game;

import java.util.Optional;

/**
 * Condición de tablas por la regla de los cincuenta movimientos.
 *
 * <p>La condición se alcanza después de cien medios movimientos
 * consecutivos sin movimiento de peón ni captura.
 */
public final class FiftyMoveRule implements EndCondition {

    private static final int HALF_MOVES_LIMIT = 100;

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        if (context.position().halfmoveClock() < HALF_MOVES_LIMIT) {
            return Optional.empty();
        }

        return Optional.of(
                GameResult.draw(
                        "Regla de los 50 movimientos"
                )
        );
    }
}