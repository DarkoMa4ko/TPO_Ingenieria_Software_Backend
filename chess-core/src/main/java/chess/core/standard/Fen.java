package chess.core.standard;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Piece;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.movement.CastlingSpec;
import chess.core.movement.PawnMovement;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Fen {

    private final Map<Character, PieceType> bySymbol = new HashMap<>();
    private final List<CastlingSpec> castling;

    public Fen(Collection<PieceType> pieceTypes, List<CastlingSpec> castling) {
        for (PieceType type : pieceTypes) {
            bySymbol.put(type.symbol(), type);
        }
        this.castling = List.copyOf(castling);
    }

    public static Fen standard() {
        return new Fen(StandardPieces.ALL, StandardPieces.CASTLING);
    }

    public Position parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("FEN vacío");
        }

        String[] fields = text.trim().split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException(
                    "Un FEN debe contener exactamente 6 campos, pero contiene " + fields.length
            );
        }

        String[] rows = fields[0].split("/");
        int ranks = rows.length;
        int files = widthOf(rows[0]);
        BoardGeometry geometry = BoardGeometry.of(files, ranks);
        Position.Builder builder = Position.builder(geometry);

        for (int i = 0; i < ranks; i++) {
            placeRow(builder, rows[i], ranks - 1 - i, files);
        }

        Color sideToMove = parseSide(fields[1]);
        builder.sideToMove(sideToMove);
        builder.unmoved(parseCastling(builder, geometry, fields[2]));
        builder.enPassantTarget(parseEnPassant(builder, geometry, sideToMove, fields[3]));
        builder.halfmoveClock(parseHalfmoveClock(fields[4]));
        builder.fullmoveNumber(parseFullmoveNumber(fields[5]));

        return builder.build();
    }

    private void placeRow(Position.Builder builder, String row, int rank, int files) {
        int file = 0;
        int i = 0;

        while (i < row.length()) {
            char character = row.charAt(i);
            if (Character.isDigit(character)) {
                int empty = 0;
                while (i < row.length() && Character.isDigit(row.charAt(i))) {
                    empty = empty * 10 + (row.charAt(i) - '0');
                    i++;
                }
                file += empty;
            } else {
                PieceType type = bySymbol.get(Character.toUpperCase(character));
                if (type == null) {
                    throw new IllegalArgumentException("Pieza desconocida en FEN: '" + character + "'");
                }
                if (file >= files) {
                    throw new IllegalArgumentException("Fila demasiado larga en FEN: " + row);
                }
                Color color = Character.isUpperCase(character) ? Color.WHITE : Color.BLACK;
                builder.place(new Square(file, rank), new Piece(type, color));
                file++;
                i++;
            }
        }

        if (file != files) {
            throw new IllegalArgumentException(
                    "La fila '" + row + "' no tiene " + files + " columnas"
            );
        }
    }

    private static int widthOf(String row) {
        int width = 0;
        int i = 0;
        while (i < row.length()) {
            if (Character.isDigit(row.charAt(i))) {
                int number = 0;
                while (i < row.length() && Character.isDigit(row.charAt(i))) {
                    number = number * 10 + (row.charAt(i) - '0');
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
            Position.Builder builder,
            BoardGeometry geometry,
            Color sideToMove,
            String field
    ) {
        if ("-".equals(field)) {
            return null;
        }

        Square target = Square.parse(field);
        if (!geometry.contains(target)) {
            throw new IllegalArgumentException(
                    "La casilla de captura al paso " + field
                            + " no existe en un tablero " + geometry
            );
        }
        if (builder.pieceAt(target).isPresent()) {
            throw new IllegalArgumentException(
                    "La casilla de captura al paso debe estar vacía: " + field
            );
        }

        int expectedRank = sideToMove == Color.WHITE
                ? geometry.ranks() - 3
                : 2;
        if (target.rank() != expectedRank) {
            throw new IllegalArgumentException("Fila inválida para captura al paso: " + field);
        }

        int movedPawnRank = target.rank() - sideToMove.forward();
        Square movedPawnSquare = new Square(target.file(), movedPawnRank);
        if (!geometry.contains(movedPawnSquare)) {
            throw new IllegalArgumentException(
                    "La captura al paso no corresponde a una casilla válida"
            );
        }

        Piece movedPawn = builder.pieceAt(movedPawnSquare)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el peón que habilita la captura al paso en " + movedPawnSquare
                ));

        boolean opponentPawn = movedPawn.color() == sideToMove.opposite()
                && movedPawn.type().movement() instanceof PawnMovement;
        if (!opponentPawn) {
            throw new IllegalArgumentException(
                    "La captura al paso no corresponde a un peón rival en " + movedPawnSquare
            );
        }
        return target;
    }

    private static Color parseSide(String field) {
        if ("w".equals(field)) return Color.WHITE;
        if ("b".equals(field)) return Color.BLACK;
        throw new IllegalArgumentException("Turno inválido en FEN: '" + field + "'");
    }

    private static int parseHalfmoveClock(String field) {
        final int value;
        try {
            value = Integer.parseInt(field);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Contador de medios movimientos inválido en FEN: '" + field + "'",
                    exception
            );
        }
        if (value < 0) {
            throw new IllegalArgumentException(
                    "El contador de medios movimientos no puede ser negativo: " + value
            );
        }
        return value;
    }

    private static int parseFullmoveNumber(String field) {
        final int value;
        try {
            value = Integer.parseInt(field);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Número completo de jugada inválido en FEN: '" + field + "'",
                    exception
            );
        }
        if (value < 1) {
            throw new IllegalArgumentException(
                    "El número completo de jugada debe ser mayor o igual que 1"
            );
        }
        return value;
    }

    private Bitboard parseCastling(
            Position.Builder builder,
            BoardGeometry geometry,
            String field
    ) {
        Bitboard unmoved = Bitboard.EMPTY;
        if ("-".equals(field)) return unmoved;

        for (char character : field.toCharArray()) {
            Color color = Character.isUpperCase(character) ? Color.WHITE : Color.BLACK;
            char upper = Character.toUpperCase(character);
            boolean kingSide = upper == 'K';
            if (upper != 'K' && upper != 'Q') {
                throw new IllegalArgumentException(
                        "Derechos de enroque inválidos en FEN: '" + field + "'"
                );
            }

            CastlingSpec spec = castling.stream()
                    .filter(candidate -> candidate.color() == color
                            && candidate.isKingSide() == kingSide)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Esta variante no tiene enroque '" + character + "'"
                    ));

            if (hasPiece(builder, spec.kingFrom(), color)
                    && hasPiece(builder, spec.rookFrom(), color)) {
                unmoved = unmoved
                        .or(geometry.bit(spec.kingFrom()))
                        .or(geometry.bit(spec.rookFrom()));
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
                .filter(piece -> piece.color() == color)
                .isPresent();
    }

    public String format(Position position) {
        BoardGeometry geometry = position.geometry();
        StringBuilder result = new StringBuilder();

        for (int rank = geometry.ranks() - 1; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < geometry.files(); file++) {
                Optional<Piece> piece = position.pieceAt(new Square(file, rank));
                if (piece.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) {
                        result.append(empty);
                        empty = 0;
                    }
                    result.append(piece.get().symbol());
                }
            }
            if (empty > 0) result.append(empty);
            if (rank > 0) result.append('/');
        }

        result.append(' ').append(position.sideToMove() == Color.WHITE ? 'w' : 'b');
        result.append(' ').append(castlingField(position));
        result.append(' ').append(position.enPassantTarget().map(Square::toString).orElse("-"));
        result.append(' ').append(position.halfmoveClock());
        result.append(' ').append(position.fullmoveNumber());
        return result.toString();
    }

    private String castlingField(Position position) {
        StringBuilder result = new StringBuilder();
        castling.stream()
                .sorted(Comparator
                        .comparing((CastlingSpec spec) -> spec.color())
                        .thenComparing(spec -> !spec.isKingSide()))
                .forEach(spec -> {
                    BoardGeometry geometry = position.geometry();
                    Bitboard origins = geometry.bit(spec.kingFrom())
                            .or(geometry.bit(spec.rookFrom()));
                    boolean unmoved = position.unmoved().and(origins).equals(origins);
                    boolean present = position.pieceAt(spec.kingFrom())
                            .filter(piece -> piece.color() == spec.color())
                            .isPresent()
                            && position.pieceAt(spec.rookFrom())
                            .filter(piece -> piece.color() == spec.color())
                            .isPresent();
                    if (unmoved && present) {
                        char letter = spec.isKingSide() ? 'K' : 'Q';
                        result.append(spec.color() == Color.WHITE
                                ? letter
                                : Character.toLowerCase(letter));
                    }
                });
        return result.length() == 0 ? "-" : result.toString();
    }
}
