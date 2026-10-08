package chess.core.board;

/**
 * Casilla del tablero, identificada por columna (file) y fila (rank), ambas base 0.
 * La casilla a1 es (0, 0) y la h8 es (7, 7) en un tablero estándar.
 *
 * <p>Es un objeto de valor inmutable. No conoce el tamaño del tablero: eso lo valida
 * {@link BoardGeometry}.
 */
public record Square(int file, int rank) {

    private static final int MAX_FILES = 26; // a..z

    public Square {
        if (file < 0 || file >= MAX_FILES || rank < 0) {
            throw new IllegalArgumentException("Casilla inválida: columna=" + file + ", fila=" + rank);
        }
    }

    /** Interpreta notación algebraica: "e4", "a1", "j10". */
    public static Square parse(String text) {
        if (text == null || text.length() < 2) {
            throw new IllegalArgumentException("Casilla inválida: '" + text + "'");
        }
        char column = Character.toLowerCase(text.charAt(0));
        if (column < 'a' || column > 'z') {
            throw new IllegalArgumentException("Casilla inválida: '" + text + "'");
        }
        int row;
        try {
            row = Integer.parseInt(text.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Casilla inválida: '" + text + "'");
        }
        if (row < 1) {
            throw new IllegalArgumentException("Casilla inválida: '" + text + "'");
        }
        return new Square(column - 'a', row - 1);
    }

    @Override
    public String toString() {
        return String.valueOf((char) ('a' + file)) + (rank + 1);
    }
}
