package chess.core.move;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.model.Move;
import chess.core.model.Position;

import java.util.function.Consumer;

/** Movimiento común: la pieza va de {@code from} a {@code to}, capturando lo que haya en destino. */
public record StandardMove(Square from, Square to) implements Move {

    @Override
    public void execute(Position.Builder board) {
        board.relocate(from, to);
    }

    /** Atajo para las reglas de movimiento: un {@code StandardMove} por cada casilla de {@code targets}. */
    public static void toEach(Square from, Bitboard targets, BoardGeometry geometry, Consumer<Move> out) {
        targets.forEachBit(index -> out.accept(new StandardMove(from, geometry.square(index))));
    }
}
