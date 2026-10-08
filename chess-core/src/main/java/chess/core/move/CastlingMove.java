package chess.core.move;

import chess.core.board.Square;
import chess.core.model.Move;
import chess.core.model.Position;

/**
 * Enroque: el rey va de {@code from} a {@code to} y, en el mismo movimiento, la torre va de
 * {@code rookFrom} a {@code rookTo}. {@code from}/{@code to} son los del rey.
 */
public record CastlingMove(Square from, Square to, Square rookFrom, Square rookTo) implements Move {

    @Override
    public void execute(Position.Builder board) {
        board.relocate(from, to);
        board.relocate(rookFrom, rookTo);
    }
}
