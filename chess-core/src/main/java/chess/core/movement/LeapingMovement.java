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
 * Movimiento "de salto": un único paso a cada una de las direcciones dadas, sin importar lo
 * que haya en el medio (caballo y rey).
 *
 * <p>Es la forma genérica de las líneas del artículo para el caballo:
 * cada salto es un shift con su máscara de columnas ({@link BoardGeometry#shift}) y el
 * resultado es la unión de todos.
 */
public final class LeapingMovement implements MovementRule {

    private final List<Direction> jumps;

    public LeapingMovement(List<Direction> jumps) {
        this.jumps = List.copyOf(jumps);
    }

    @Override
    public Bitboard attacks(Position position, Square from, Color color) {
        BoardGeometry geometry = position.geometry();
        Bitboard origin = geometry.bit(from);
        Bitboard attacked = Bitboard.EMPTY;
        for (Direction jump : jumps) {
            attacked = attacked.or(geometry.shift(origin, jump));
        }
        return attacked;
    }

    @Override
    public void generate(Position position, Square from, Color color, Consumer<Move> out) {
        Bitboard targets = attacks(position, from, color).andNot(position.occupiedBy(color));
        StandardMove.toEach(from, targets, position.geometry(), out);
    }
}
