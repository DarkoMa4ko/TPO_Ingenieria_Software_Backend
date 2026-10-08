package chess.core.game;

import chess.core.model.Move;
import chess.core.model.Position;

import java.util.List;
import java.util.Optional;

/**
 * Estrategia que decide si una posición termina la partida (jaque mate, ahogado, y en el
 * futuro: regla de los 50 movimientos, repetición, material insuficiente, ...).
 *
 * <p>{@code Game} recibe una lista de estas condiciones: agregar una regla de fin de partida
 * es agregar una clase y registrarla, sin tocar a {@code Game}.
 */
public interface EndCondition {

    /**
     * @param position   posición a evaluar (le toca mover a {@code position.sideToMove()})
     * @param legalMoves movimientos legales de esa posición, ya calculados
     * @return el resultado si la partida terminó, vacío si continúa
     */
    Optional<GameResult> evaluate(Position position, List<Move> legalMoves);
}
