package chess.core.move;

import chess.core.board.Square;
import chess.core.model.Move;
import chess.core.model.Position;

/**
 * Avance doble del peón desde su fila inicial. Además de mover la pieza, deja anotada la
 * casilla que saltó para que el rival pueda capturar al paso en el turno siguiente.
 */
public record DoublePushMove(Square from, Square to, Square enPassantTarget) implements Move {

    @Override
    public void execute(Position.Builder board) {
        board.relocate(from, to);
        board.enPassantTarget(enPassantTarget);
    }
}
