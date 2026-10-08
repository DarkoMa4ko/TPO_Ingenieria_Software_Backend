package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.Square;

import java.util.function.Consumer;

/**
 * Estrategia (patrón Strategy) que define cómo se mueve una pieza.
 *
 * <p>Una regla no sabe qué pieza es ni de qué color; solo responde dos preguntas sobre una
 * posición concreta:
 * <ul>
 *   <li>{@link #attacks}: ¿qué casillas controla desde {@code from}? Se usa para detectar jaque
 *       y para validar el enroque. Es un {@link Bitboard}, no una lista.</li>
 *   <li>{@link #generate}: ¿qué movimientos pseudo-legales tiene? "Pseudo-legal" significa que
 *       respeta el movimiento de la pieza pero todavía no se filtró que deje al propio rey en jaque.</li>
 * </ul>
 *
 * <p>Las piezas nuevas se crean <b>componiendo</b> reglas existentes (ver CompositeMovement) o
 * escribiendo una regla nueva, sin modificar ninguna clase ya hecha.
 */
public interface MovementRule {

    /** Casillas que la pieza controla desde {@code from}, ocupadas o no, sin filtrar por color. */
    Bitboard attacks(Position position, Square from, Color color);

    /** Entrega a {@code out} cada movimiento pseudo-legal de la pieza ubicada en {@code from}. */
    void generate(Position position, Square from, Color color, Consumer<Move> out);
}
