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
 * Lectura y escritura de posiciones en notación FEN ("Forsyth–Edwards").
 *
 * <p>Es un códec configurable: recibe los tipos de pieza y los enroques que reconoce, así que
 * sirve también para variantes con otras piezas. El tamaño del tablero se deduce del texto
 * (cantidad de filas y de columnas), por lo que también funciona con tableros no estándar.
 *
 * <p>Limitación deliberada: los contadores de medio-movimiento y de jugadas no se almacenan
 * (el núcleo no implementa la regla de los 50 movimientos); al escribir se emite "0 1".
 */
public final class Fen {

    private final Map<Character, PieceType> bySymbol = new HashMap<>();
    private final List<CastlingSpec> castling;

    public Fen(Collection<PieceType> pieceTypes, List<CastlingSpec> castling) {
        for (PieceType type : pieceTypes) {
            bySymbol.put(type.symbol(), type);
        }
        this.castling = List.copyOf(castling);
    }

    /** Códec para las piezas y enroques del ajedrez clásico. */
    public static Fen standard() {
        return new Fen(StandardPieces.ALL, StandardPieces.CASTLING);
    }

    // ---- lectura -------------------------------------------------------------------------

    public Position parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("FEN vacío");
        }
        String[] fields = text.trim().split("\\s+");
        String[] rows = fields[0].split("/");
        int ranks = rows.length;
        int files = widthOf(rows[0]);
        BoardGeometry geometry = BoardGeometry.of(files, ranks);

        Position.Builder builder = Position.builder(geometry);
        for (int i = 0; i < ranks; i++) {
            placeRow(builder, rows[i], ranks - 1 - i, files);
        }

        builder.sideToMove(parseSide(fields.length > 1 ? fields[1] : "w"));
        builder.unmoved(parseCastling(builder, geometry, fields.length > 2 ? fields[2] : "-"));
        builder.enPassantTarget(parseEnPassant(geometry, fields.length > 3 ? fields[3] : "-"));
        return builder.build();
    }

    private void placeRow(Position.Builder builder, String row, int rank, int files) {
        int file = 0;
        int i = 0;
        while (i < row.length()) {
            char ch = row.charAt(i);
            if (Character.isDigit(ch)) {
                int empty = 0;
                while (i < row.length() && Character.isDigit(row.charAt(i))) {
                    empty = empty * 10 + (row.charAt(i) - '0');
                    i++;
                }
                file += empty;
            } else {
                PieceType type = bySymbol.get(Character.toUpperCase(ch));
                if (type == null) {
                    throw new IllegalArgumentException("Pieza desconocida en FEN: '" + ch + "'");
                }
                if (file >= files) {
                    throw new IllegalArgumentException("Fila demasiado larga en FEN: " + row);
                }
                Color color = Character.isUpperCase(ch) ? Color.WHITE : Color.BLACK;
                builder.place(new Square(file, rank), new Piece(type, color));
                file++;
                i++;
            }
        }
        if (file != files) {
            throw new IllegalArgumentException("La fila '" + row + "' no tiene " + files + " columnas");
        }
    }

    private static int widthOf(String row) {
        int width = 0;
        int i = 0;
        while (i < row.length()) {
            if (Character.isDigit(row.charAt(i))) {
                int n = 0;
                while (i < row.length() && Character.isDigit(row.charAt(i))) {
                    n = n * 10 + (row.charAt(i) - '0');
                    i++;
                }
                width += n;
            } else {
                width++;
                i++;
            }
        }
        return width;
    }

    private static Square parseEnPassant(BoardGeometry geometry, String field) {
        if ("-".equals(field)) {
            return null;
        }
        Square square = Square.parse(field);
        if (!geometry.contains(square)) {
            throw new IllegalArgumentException("La casilla de captura al paso " + field + " no existe en un tablero " + geometry);
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
        throw new IllegalArgumentException("Turno inválido en FEN: '" + field + "'");
    }

    /** Traduce "KQkq" al conjunto de casillas de rey y torre que nunca se movieron. */
    private Bitboard parseCastling(Position.Builder builder, BoardGeometry geometry, String field) {
        Bitboard unmoved = Bitboard.EMPTY;
        if ("-".equals(field)) {
            return unmoved;
        }
        for (char ch : field.toCharArray()) {
            Color color = Character.isUpperCase(ch) ? Color.WHITE : Color.BLACK;
            boolean kingSide = Character.toUpperCase(ch) == 'K';
            if (Character.toUpperCase(ch) != 'K' && Character.toUpperCase(ch) != 'Q') {
                throw new IllegalArgumentException("Derechos de enroque inválidos en FEN: '" + field + "'");
            }
            CastlingSpec spec = castling.stream()
                    .filter(s -> s.color() == color && s.isKingSide() == kingSide)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Esta variante no tiene enroque '" + ch + "'"));
            if (hasPiece(builder, spec.kingFrom(), color) && hasPiece(builder, spec.rookFrom(), color)) {
                unmoved = unmoved.or(geometry.bit(spec.kingFrom())).or(geometry.bit(spec.rookFrom()));
            }
        }
        return unmoved;
    }

    private static boolean hasPiece(Position.Builder builder, Square square, Color color) {
        return builder.pieceAt(square).filter(p -> p.color() == color).isPresent();
    }

    // ---- escritura -----------------------------------------------------------------------

    public String format(Position position) {
        BoardGeometry geometry = position.geometry();
        StringBuilder sb = new StringBuilder();
        for (int rank = geometry.ranks() - 1; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < geometry.files(); file++) {
                Optional<Piece> piece = position.pieceAt(new Square(file, rank));
                if (piece.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(piece.get().symbol());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(position.sideToMove() == Color.WHITE ? 'w' : 'b');
        sb.append(' ').append(castlingField(position));
        sb.append(' ').append(position.enPassantTarget().map(Square::toString).orElse("-"));
        sb.append(" 0 1");
        return sb.toString();
    }

    private String castlingField(Position position) {
        StringBuilder sb = new StringBuilder();
        castling.stream()
                .sorted(Comparator.comparing((CastlingSpec s) -> s.color())
                        .thenComparing(s -> !s.isKingSide()))
                .forEach(spec -> {
                    BoardGeometry g = position.geometry();
                    Bitboard origins = g.bit(spec.kingFrom()).or(g.bit(spec.rookFrom()));
                    boolean unmoved = position.unmoved().and(origins).equals(origins);
                    boolean present = position.pieceAt(spec.kingFrom()).filter(p -> p.color() == spec.color()).isPresent()
                            && position.pieceAt(spec.rookFrom()).filter(p -> p.color() == spec.color()).isPresent();
                    if (unmoved && present) {
                        char letter = spec.isKingSide() ? 'K' : 'Q';
                        sb.append(spec.color() == Color.WHITE ? letter : Character.toLowerCase(letter));
                    }
                });
        return sb.length() == 0 ? "-" : sb.toString();
    }
}
