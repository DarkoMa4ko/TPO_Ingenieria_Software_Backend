package chess.cli;

import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.model.Piece;
import chess.core.model.Position;

import java.util.Optional;

/**
 * Dibuja una {@link Position} como texto, con las blancas abajo. Mayúsculas = blancas,
 * minúsculas = negras, punto = casilla vacía. Se adapta al tamaño del tablero.
 */
public final class BoardRenderer {

    public String render(Position position) {
        BoardGeometry geometry = position.geometry();
        int labelWidth = String.valueOf(geometry.ranks()).length();
        StringBuilder sb = new StringBuilder();
        sb.append(fileLabels(geometry, labelWidth)).append('\n');
        for (int rank = geometry.ranks() - 1; rank >= 0; rank--) {
            String label = String.format("%" + labelWidth + "d", rank + 1);
            sb.append(label);
            for (int file = 0; file < geometry.files(); file++) {
                Optional<Piece> piece = position.pieceAt(new Square(file, rank));
                sb.append(' ').append(piece.map(Piece::symbol).orElse('.'));
            }
            sb.append(' ').append(label).append('\n');
        }
        sb.append(fileLabels(geometry, labelWidth));
        return sb.toString();
    }

    private static String fileLabels(BoardGeometry geometry, int labelWidth) {
        StringBuilder sb = new StringBuilder(" ".repeat(labelWidth));
        for (int file = 0; file < geometry.files(); file++) {
            sb.append(' ').append((char) ('a' + file));
        }
        return sb.toString();
    }
}
