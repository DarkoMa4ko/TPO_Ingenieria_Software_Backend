package chess.core.move;

import chess.core.board.Square;
import chess.core.model.Move;
import chess.core.model.Position;

/**
 * Captura al paso: el peón se mueve en diagonal a una casilla vacía y captura al peón rival
 * que está en {@code captured} (al costado del origen, no en el destino).
 */
public record EnPassantMove(Square from, Square to, Square captured) implements Move {

    @Override
    public void execute(Position.Builder board) {
        board.remove(captured);
        board.relocate(from, to);
    }
}
