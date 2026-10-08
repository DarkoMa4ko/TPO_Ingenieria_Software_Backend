package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;

/**
 * Servicio que responde "¿qué movimientos tiene disponibles el jugador al que le toca?".
 * {@code Game} depende de esta interfaz y no de una implementación concreta (inversión de
 * dependencias): una variante con reglas de legalidad distintas inyecta otra.
 */
public interface MoveGenerator {

    /** Movimientos que respetan el movimiento de cada pieza, aunque dejen al propio rey en jaque. */
    List<Move> pseudoLegalMoves(Position position);

    /** Movimientos pseudo-legales que además no dejan al propio rey en jaque. */
    List<Move> legalMoves(Position position);
}
