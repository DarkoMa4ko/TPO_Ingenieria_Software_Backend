package chess.core.movement;

import chess.core.board.Square;
import chess.core.model.Color;

import java.util.Objects;

/**
 * Descripción de un enroque posible: bando, de dónde a dónde va el rey y de dónde a dónde va
 * la torre. En ajedrez estándar hay cuatro (corto y largo, de cada bando) y se declaran como
 * datos, no como código.
 */
public record CastlingSpec(Color color, Square kingFrom, Square kingTo, Square rookFrom, Square rookTo) {

    public CastlingSpec {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(kingFrom, "kingFrom");
        Objects.requireNonNull(kingTo, "kingTo");
        Objects.requireNonNull(rookFrom, "rookFrom");
        Objects.requireNonNull(rookTo, "rookTo");
    }

    /** Enroque corto (hacia el lado del rey): la torre está a la derecha del rey. */
    public boolean isKingSide() {
        return rookFrom.file() > kingFrom.file();
    }
}
