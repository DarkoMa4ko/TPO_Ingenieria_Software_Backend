package chess.core.model;

/** Bando de una pieza o jugador. */
public enum Color {
    WHITE,
    BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Sentido en que avanzan los peones de este bando sobre las filas: +1 las blancas, -1 las negras. */
    public int forward() {
        return this == WHITE ? 1 : -1;
    }
}
