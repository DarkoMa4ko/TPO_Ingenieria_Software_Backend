package chess.core.standard;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Piece;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.movement.CastlingSpec;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lectura y escritura de posiciones en notación FEN
 * ("Forsyth-Edwards Notation").
 *
 * <p>Es un códec configurable: recibe los tipos de pieza y los enroques
 * que reconoce, por lo que también sirve para variantes con otras piezas.
 * El tamaño del tablero se deduce del texto, a partir de la cantidad de
 * filas y columnas, por lo que también funciona con tableros no estándar.
 *
 * <p>Además de la distribución de piezas, el turno, los derechos de enroque
 * y la captura al paso, conserva el contador de medios movimientos
 * transcurridos desde la última captura o movimiento de peón.
 *
 * <p>El número completo de jugada todavía no forma parte del modelo
 * {@link Position}; por ello, al escribir un FEN se conserva el comportamiento
 * actual y se emite {@code 1} como sexto campo.
 */
public final class Fen {

    private final Map<Character, PieceType> bySymbol =
            new HashMap<>();

    private final List<CastlingSpec> castling;

    public Fen(
            Collection<PieceType> pieceTypes,
            List<CastlingSpec> castling
    ) {
        for (PieceType type : pieceTypes) {
            bySymbol.put(
                    type.symbol(),
                    type
            );
        }

        this.castling =
                List.copyOf(castling);
    }

    /**
     * Devuelve un códec configurado para las piezas y enroques
     * del ajedrez clásico.
     */
    public static Fen standard() {
        return new Fen(
                StandardPieces.ALL,
                StandardPieces.CASTLING
        );
    }

    // ---- lectura -------------------------------------------------------------------------

    /**
     * Interpreta una posición expresada en notación FEN.
     *
     * <p>El quinto campo, cuando está presente, se interpreta como el
     * contador de medios movimientos. Si el campo no está presente,
     * el contador comienza en cero.
     *
     * @param text posición expresada en notación FEN
     * @return posición construida a partir del texto
     * @throws IllegalArgumentException si el FEN contiene datos inválidos
     */
    public Position parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "FEN vacío"
            );
        }

        String[] fields =
                text.trim().split("\\s+");

        String[] rows =
                fields[0].split("/");

        int ranks =
                rows.length;

        int files =
                widthOf(rows[0]);

        BoardGeometry geometry =
                BoardGeometry.of(files, ranks);

        Position.Builder builder =
                Position.builder(geometry);

        for (int i = 0; i < ranks; i++) {
            placeRow(
                    builder,
                    rows[i],
                    ranks - 1 - i,
                    files
            );
        }

        builder.sideToMove(
                parseSide(
                        fields.length > 1
                                ? fields[1]
                                : "w"
                )
        );

        builder.unmoved(
                parseCastling(
                        builder,
                        geometry,
                        fields.length > 2
                                ? fields[2]
                                : "-"
                )
        );

        builder.enPassantTarget(
                parseEnPassant(
                        geometry,
                        fields.length > 3
                                ? fields[3]
                                : "-"
                )
        );

        builder.halfmoveClock(
                parseHalfmoveClock(
                        fields.length > 4
                                ? fields[4]
                                : "0"
                )
        );

        return builder.build();
    }

    private void placeRow(
            Position.Builder builder,
            String row,
            int rank,
            int files
    ) {
        int file = 0;
        int i = 0;

        while (i < row.length()) {
            char character =
                    row.charAt(i);

            if (Character.isDigit(character)) {
                int empty = 0;

                while (i < row.length()
                        && Character.isDigit(
                        row.charAt(i)
                )) {

                    empty =
                            empty * 10
                                    + (row.charAt(i) - '0');

                    i++;
                }

                file += empty;
            } else {
                PieceType type =
                        bySymbol.get(
                                Character.toUpperCase(
                                        character
                                )
                        );

                if (type == null) {
                    throw new IllegalArgumentException(
                            "Pieza desconocida en FEN: '"
                                    + character
                                    + "'"
                    );
                }

                if (file >= files) {
                    throw new IllegalArgumentException(
                            "Fila demasiado larga en FEN: "
                                    + row
                    );
                }

                Color color =
                        Character.isUpperCase(character)
                                ? Color.WHITE
                                : Color.BLACK;

                builder.place(
                        new Square(file, rank),
                        new Piece(type, color)
                );

                file++;
                i++;
            }
        }

        if (file != files) {
            throw new IllegalArgumentException(
                    "La fila '"
                            + row
                            + "' no tiene "
                            + files
                            + " columnas"
            );
        }
    }

    private static int widthOf(String row) {
        int width = 0;
        int i = 0;

        while (i < row.length()) {
            if (Character.isDigit(row.charAt(i))) {
                int number = 0;

                while (i < row.length()
                        && Character.isDigit(
                        row.charAt(i)
                )) {

                    number =
                            number * 10
                                    + (row.charAt(i) - '0');

                    i++;
                }

                width += number;
            } else {
                width++;
                i++;
            }
        }

        return width;
    }

    private static Square parseEnPassant(
            BoardGeometry geometry,
            String field
    ) {
        if ("-".equals(field)) {
            return null;
        }

        Square square =
                Square.parse(field);

        if (!geometry.contains(square)) {
            throw new IllegalArgumentException(
                    "La casilla de captura al paso "
                            + field
                            + " no existe en un tablero "
                            + geometry
            );
        }

        return square;
    }

    private static Color parseSide(String field) {
        if ("w".equals(field)) {
            return Color.WHITE;
        }

        if ("b".equals(field)) {
            return Color.BLACK;
        }

        throw new IllegalArgumentException(
                "Turno inválido en FEN: '"
                        + field
                        + "'"
        );
    }

    /**
     * Interpreta el quinto campo del FEN como cantidad de medios
     * movimientos desde la última captura o movimiento de peón.
     */
    private static int parseHalfmoveClock(String field) {
        final int halfmoveClock;

        try {
            halfmoveClock =
                    Integer.parseInt(field);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Contador de medios movimientos inválido en FEN: '"
                            + field
                            + "'",
                    exception
            );
        }

        if (halfmoveClock < 0) {
            throw new IllegalArgumentException(
                    "El contador de medios movimientos no puede ser negativo: "
                            + halfmoveClock
            );
        }

        return halfmoveClock;
    }

    /**
     * Traduce el campo de derechos de enroque al conjunto de casillas
     * de rey y torre que todavía no se movieron.
     */
    private Bitboard parseCastling(
            Position.Builder builder,
            BoardGeometry geometry,
            String field
    ) {
        Bitboard unmoved =
                Bitboard.EMPTY;

        if ("-".equals(field)) {
            return unmoved;
        }

        for (char character : field.toCharArray()) {
            Color color =
                    Character.isUpperCase(character)
                            ? Color.WHITE
                            : Color.BLACK;

            char upper =
                    Character.toUpperCase(character);

            boolean kingSide =
                    upper == 'K';

            if (upper != 'K' && upper != 'Q') {
                throw new IllegalArgumentException(
                        "Derechos de enroque inválidos en FEN: '"
                                + field
                                + "'"
                );
            }

            CastlingSpec spec = castling.stream()
                    .filter(candidate ->
                            candidate.color() == color
                                    && candidate.isKingSide()
                                    == kingSide
                    )
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Esta variante no tiene enroque '"
                                            + character
                                            + "'"
                            )
                    );

            if (hasPiece(
                    builder,
                    spec.kingFrom(),
                    color
            ) && hasPiece(
                    builder,
                    spec.rookFrom(),
                    color
            )) {
                unmoved = unmoved
                        .or(
                                geometry.bit(
                                        spec.kingFrom()
                                )
                        )
                        .or(
                                geometry.bit(
                                        spec.rookFrom()
                                )
                        );
            }
        }

        return unmoved;
    }

    private static boolean hasPiece(
            Position.Builder builder,
            Square square,
            Color color
    ) {
        return builder.pieceAt(square)
                .filter(piece ->
                        piece.color() == color
                )
                .isPresent();
    }

    // ---- escritura -----------------------------------------------------------------------

    /**
     * Convierte una posición en notación FEN.
     *
     * <p>El quinto campo utiliza el contador de medios movimientos
     * almacenado en la posición. El sexto campo continúa emitiéndose
     * como {@code 1}, ya que el número completo de jugada todavía
     * no forma parte del modelo.
     */
    public String format(Position position) {
        BoardGeometry geometry =
                position.geometry();

        StringBuilder result =
                new StringBuilder();

        for (int rank = geometry.ranks() - 1;
             rank >= 0;
             rank--) {

            int empty = 0;

            for (int file = 0;
                 file < geometry.files();
                 file++) {

                Optional<Piece> piece =
                        position.pieceAt(
                                new Square(file, rank)
                        );

                if (piece.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) {
                        result.append(empty);
                        empty = 0;
                    }

                    result.append(
                            piece.get().symbol()
                    );
                }
            }

            if (empty > 0) {
                result.append(empty);
            }

            if (rank > 0) {
                result.append('/');
            }
        }

        result.append(' ')
                .append(
                        position.sideToMove() == Color.WHITE
                                ? 'w'
                                : 'b'
                );

        result.append(' ')
                .append(
                        castlingField(position)
                );

        result.append(' ')
                .append(
                        position.enPassantTarget()
                                .map(Square::toString)
                                .orElse("-")
                );

        result.append(' ')
                .append(
                        position.halfmoveClock()
                );

        // El número completo de jugada todavía no está modelado.
        result.append(" 1");

        return result.toString();
    }

    private String castlingField(Position position) {
        StringBuilder result =
                new StringBuilder();

        castling.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        (CastlingSpec spec) ->
                                                spec.color()
                                )
                                .thenComparing(
                                        spec ->
                                                !spec.isKingSide()
                                )
                )
                .forEach(spec -> {
                    BoardGeometry geometry =
                            position.geometry();

                    Bitboard origins =
                            geometry.bit(
                                    spec.kingFrom()
                            ).or(
                                    geometry.bit(
                                            spec.rookFrom()
                                    )
                            );

                    boolean unmoved =
                            position.unmoved()
                                    .and(origins)
                                    .equals(origins);

                    boolean present =
                            position.pieceAt(
                                            spec.kingFrom()
                                    )
                                    .filter(piece ->
                                            piece.color()
                                                    == spec.color()
                                    )
                                    .isPresent()
                                    &&
                                    position.pieceAt(
                                                    spec.rookFrom()
                                            )
                                            .filter(piece ->
                                                    piece.color()
                                                            == spec.color()
                                            )
                                            .isPresent();

                    if (unmoved && present) {
                        char letter =
                                spec.isKingSide()
                                        ? 'K'
                                        : 'Q';

                        result.append(
                                spec.color() == Color.WHITE
                                        ? letter
                                        : Character.toLowerCase(
                                        letter
                                )
                        );
                    }
                });

        return result.length() == 0
                ? "-"
                : result.toString();
    }
}