package chess.core.game;

import java.util.Optional;

/**
 * Condición de fin de partida que aplica la regla de los cincuenta movimientos.
 *
 * <p>La partida termina en tablas cuando se producen cien medios movimientos
 * consecutivos (cincuenta jugadas de cada bando) sin mover peones ni capturar:
 * el reloj de medio movimientos de la posición alcanza 100.
 */
public final class FiftyMoveRule implements EndCondition {

    @Override
    public Optional<GameResult> evaluate(GameContext context) {
        if (context.position().halfmoveClock() >= 100) {
            return Optional.of(GameResult.draw("Regla de los 50 movimientos"));
        }

        return Optional.empty();
    }
}
