package chess.core.movement;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Direction;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.MovementRule;
import chess.core.model.Position;
import chess.core.move.StandardMove;

import java.util.List;
import java.util.function.Consumer;

/**
 * Movimiento "deslizante": avanza en línea recta por cada dirección hasta chocar con una
 * pieza o con el borde (torre, alfil, dama).
 *
 * <p>Técnica (clásica de bitboards, "ray attacks"): para cada dirección hay un <i>rayo</i>
 * precalculado desde cada casilla. Se intersecta el rayo con las casillas ocupadas para hallar
 * el primer bloqueo (el bit más bajo si la dirección sube de índice, el más alto si baja) y se
 * recorta el rayo que queda "detrás" de ese bloqueo. El bloqueo mismo sigue en el conjunto de
 * ataque: si es enemigo, se puede capturar.
 */
public final class SlidingMovement implements MovementRule {

    private final List<Direction> directions;

    public SlidingMovement(List<Direction> directions) {
        this.directions = List.copyOf(directions);
    }

    @Override
    public Bitboard attacks(Position position, Square from, Color color) {
        BoardGeometry geometry = position.geometry();
        int origin = geometry.index(from);
        Bitboard occupied = position.occupied();
        Bitboard attacked = Bitboard.EMPTY;
        for (Direction direction : directions) {
            Bitboard ray = geometry.ray(origin, direction);
            Bitboard blockers = ray.and(occupied);
            if (!blockers.isEmpty()) {
                int firstBlocker = geometry.isIncreasing(direction) ? blockers.lowestBit() : blockers.highestBit();
                ray = ray.andNot(geometry.ray(firstBlocker, direction));
            }
            attacked = attacked.or(ray);
        }
        return attacked;
    }

    @Override
    public void generate(Position position, Square from, Color color, Consumer<Move> out) {
        Bitboard targets = attacks(position, from, color).andNot(position.occupiedBy(color));
        StandardMove.toEach(from, targets, position.geometry(), out);
    }
}
