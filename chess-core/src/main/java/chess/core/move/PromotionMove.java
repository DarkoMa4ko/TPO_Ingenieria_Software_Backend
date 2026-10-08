package chess.core.move;

import chess.core.board.Square;
import chess.core.model.Move;
import chess.core.model.Piece;
import chess.core.model.PieceType;
import chess.core.model.Position;

import java.util.Optional;

/** Un peón llega a la última fila (con o sin captura) y se convierte en {@code promoteTo}. */
public record PromotionMove(Square from, Square to, PieceType promoteTo) implements Move {

    @Override
    public void execute(Position.Builder board) {
        Piece pawn = board.pieceAt(from)
                .orElseThrow(() -> new IllegalStateException("No hay peón en " + from));
        board.remove(from);
        board.place(to, new Piece(promoteTo, pawn.color()));
    }

    @Override
    public Optional<PieceType> promotion() {
        return Optional.of(promoteTo);
    }
}
