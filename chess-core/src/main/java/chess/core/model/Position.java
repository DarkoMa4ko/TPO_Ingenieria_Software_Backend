package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Estado completo e <b>inmutable</b> de una partida en un instante: dónde está cada pieza, a
 * quién le toca, qué piezas nunca se movieron (base del derecho a enrocar) y si hay una
 * casilla de captura al paso disponible.
 *
 * <p>Las piezas se guardan como bitboards: uno por color y uno por tipo de pieza. Para saber
 * qué hay en una casilla se cruzan (AND) esos conjuntos.
 *
 * <p>Que sea inmutable permite probar un movimiento ({@link #play}) sin "deshacerlo" después,
 * compartir posiciones entre hilos sin riesgo y guardar el historial completo de la partida.
 * Los cambios se arman con un {@link Builder}.
 */
public final class Position {

    private final BoardGeometry geometry;
    private final Map<PieceType, Bitboard> byType;
    private final Map<Color, Bitboard> byColor;
    private final Bitboard occupied;
    private final Color sideToMove;
    private final Bitboard unmoved;
    private final Square enPassantTarget;

    // Caché perezosa del mapa de ataques de cada bando. La posición es inmutable, así que nunca
    // se invalida; si dos hilos la calculan a la vez obtienen el mismo valor (carrera benigna).
    private volatile Bitboard attackedByWhite;
    private volatile Bitboard attackedByBlack;

    private Position(Builder b) {
        this.geometry = b.geometry;
        Map<PieceType, Bitboard> types = new LinkedHashMap<>();
        b.byType.forEach((type, board) -> {
            if (!board.isEmpty()) {
                types.put(type, board);
            }
        });
        this.byType = Collections.unmodifiableMap(types);
        Map<Color, Bitboard> colors = new EnumMap<>(Color.class);
        colors.put(Color.WHITE, b.byColor.get(Color.WHITE));
        colors.put(Color.BLACK, b.byColor.get(Color.BLACK));
        this.byColor = Collections.unmodifiableMap(colors);
        this.occupied = colors.get(Color.WHITE).or(colors.get(Color.BLACK));
        this.sideToMove = b.sideToMove;
        this.unmoved = b.unmoved;
        this.enPassantTarget = b.enPassantTarget;
    }

    /** Tablero vacío, con el turno de las blancas, listo para ir colocando piezas. */
    public static Builder builder(BoardGeometry geometry) {
        return new Builder(geometry);
    }

    /** Copia modificable de esta posición. */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // ---- consultas -----------------------------------------------------------------------

    public BoardGeometry geometry() {
        return geometry;
    }

    public Color sideToMove() {
        return sideToMove;
    }

    /** Todas las casillas ocupadas. */
    public Bitboard occupied() {
        return occupied;
    }

    public Bitboard occupiedBy(Color color) {
        return byColor.get(color);
    }

    /** Casillas de las piezas del tipo dado, de ambos colores. */
    public Bitboard pieces(PieceType type) {
        return byType.getOrDefault(type, Bitboard.EMPTY);
    }

    /** Casillas de las piezas del tipo y color dados. */
    public Bitboard pieces(Color color, PieceType type) {
        return pieces(type).and(byColor.get(color));
    }

    /** Tipos de pieza presentes actualmente en el tablero. */
    public Set<PieceType> pieceTypes() {
        return byType.keySet();
    }

    public Optional<Piece> pieceAt(Square square) {
        int index = geometry.index(square);
        Color color;
        if (byColor.get(Color.WHITE).has(index)) {
            color = Color.WHITE;
        } else if (byColor.get(Color.BLACK).has(index)) {
            color = Color.BLACK;
        } else {
            return Optional.empty();
        }
        for (Map.Entry<PieceType, Bitboard> entry : byType.entrySet()) {
            if (entry.getValue().has(index)) {
                return Optional.of(new Piece(entry.getKey(), color));
            }
        }
        throw new IllegalStateException("Inconsistencia: casilla ocupada sin tipo de pieza en " + square);
    }

    /** Casillas cuya pieza todavía no se movió (ni fue capturada ni reemplazada). */
    public Bitboard unmoved() {
        return unmoved;
    }

    /** Casilla "detrás" del peón que acaba de avanzar dos pasos, si corresponde. */
    public Optional<Square> enPassantTarget() {
        return Optional.ofNullable(enPassantTarget);
    }

    // ---- ataques y jaque -----------------------------------------------------------------

    /**
     * Unión de las casillas que controlan todas las piezas de {@code color}.
     * Se calcula una vez por posición y se reutiliza.
     */
    public Bitboard attackedBy(Color color) {
        Bitboard cached = color == Color.WHITE ? attackedByWhite : attackedByBlack;
        if (cached != null) {
            return cached;
        }
        Bitboard attacked = Bitboard.EMPTY;
        for (Map.Entry<PieceType, Bitboard> entry : byType.entrySet()) {
            MovementRule rule = entry.getKey().movement();
            for (Square from : geometry.squares(entry.getValue().and(byColor.get(color)))) {
                attacked = attacked.or(rule.attacks(this, from, color));
            }
        }
        if (color == Color.WHITE) {
            attackedByWhite = attacked;
        } else {
            attackedByBlack = attacked;
        }
        return attacked;
    }

    /** {@code true} si alguna pieza real (rey) de {@code color} está siendo atacada. */
    public boolean isInCheck(Color color) {
        Bitboard royal = Bitboard.EMPTY;
        for (PieceType type : byType.keySet()) {
            if (type.royal()) {
                royal = royal.or(pieces(color, type));
            }
        }
        return !royal.isEmpty() && royal.intersects(attackedBy(color.opposite()));
    }

    // ---- transición ----------------------------------------------------------------------

    /**
     * Devuelve la posición que resulta de aplicar {@code move}. No valida que el movimiento sea
     * legal: de eso se ocupa el generador de movimientos y {@code Game}.
     */
    public Position play(Move move) {
        Builder builder = toBuilder();
        builder.enPassantTarget(null); // el derecho de captura al paso dura un solo turno
        move.execute(builder);
        builder.sideToMove(sideToMove.opposite());
        return builder.build();
    }

    // ---- igualdad ------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position other)) {
            return false;
        }
        return geometry.equals(other.geometry)
                && sideToMove == other.sideToMove
                && byType.equals(other.byType)
                && byColor.equals(other.byColor)
                && unmoved.equals(other.unmoved)
                && Objects.equals(enPassantTarget, other.enPassantTarget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geometry, sideToMove, byType, byColor, unmoved, enPassantTarget);
    }

    // ---- constructor mutable -------------------------------------------------------------

    /**
     * Versión mutable y temporal de una {@link Position}. Es lo que reciben los {@link Move}
     * para aplicar sus cambios; al final se llama a {@link #build()}.
     */
    public static final class Builder {

        private final BoardGeometry geometry;
        private final Map<PieceType, Bitboard> byType = new LinkedHashMap<>();
        private final Map<Color, Bitboard> byColor = new EnumMap<>(Color.class);
        private Color sideToMove = Color.WHITE;
        private Bitboard unmoved = Bitboard.EMPTY;
        private Square enPassantTarget;

        private Builder(BoardGeometry geometry) {
            this.geometry = Objects.requireNonNull(geometry, "geometry");
            byColor.put(Color.WHITE, Bitboard.EMPTY);
            byColor.put(Color.BLACK, Bitboard.EMPTY);
        }

        private Builder(Position source) {
            this.geometry = source.geometry;
            this.byType.putAll(source.byType);
            this.byColor.putAll(source.byColor);
            this.sideToMove = source.sideToMove;
            this.unmoved = source.unmoved;
            this.enPassantTarget = source.enPassantTarget;
        }

        public BoardGeometry geometry() {
            return geometry;
        }

        public Optional<Piece> pieceAt(Square square) {
            int index = geometry.index(square);
            Color color;
            if (byColor.get(Color.WHITE).has(index)) {
                color = Color.WHITE;
            } else if (byColor.get(Color.BLACK).has(index)) {
                color = Color.BLACK;
            } else {
                return Optional.empty();
            }
            for (Map.Entry<PieceType, Bitboard> entry : byType.entrySet()) {
                if (entry.getValue().has(index)) {
                    return Optional.of(new Piece(entry.getKey(), color));
                }
            }
            throw new IllegalStateException("Inconsistencia: casilla ocupada sin tipo de pieza en " + square);
        }

        /** Coloca una pieza (reemplazando lo que hubiera) y marca la casilla como "ya movida". */
        public Builder place(Square square, Piece piece) {
            remove(square);
            int index = geometry.index(square);
            byType.merge(piece.type(), Bitboard.ofBit(index), Bitboard::or);
            byColor.merge(piece.color(), Bitboard.ofBit(index), Bitboard::or);
            return this;
        }

        /** Quita la pieza de la casilla (si hay) y la marca como "ya movida". */
        public Builder remove(Square square) {
            Optional<Piece> existing = pieceAt(square);
            int index = geometry.index(square);
            if (existing.isPresent()) {
                Piece piece = existing.get();
                byType.computeIfPresent(piece.type(), (t, board) -> board.without(index));
                byColor.computeIfPresent(piece.color(), (c, board) -> board.without(index));
            }
            unmoved = unmoved.without(index);
            return this;
        }

        /** Mueve la pieza de {@code from} a {@code to}; si en {@code to} había otra, queda capturada. */
        public Builder relocate(Square from, Square to) {
            Piece piece = pieceAt(from)
                    .orElseThrow(() -> new IllegalStateException("No hay pieza en " + from));
            remove(from);
            place(to, piece);
            return this;
        }

        public Builder sideToMove(Color color) {
            this.sideToMove = Objects.requireNonNull(color, "color");
            return this;
        }

        /** Define qué casillas contienen piezas que nunca se movieron (derechos de enroque). */
        public Builder unmoved(Bitboard squares) {
            this.unmoved = Objects.requireNonNull(squares, "squares");
            return this;
        }

        /** Casilla de captura al paso disponible para el próximo turno, o {@code null} si no hay. */
        public Builder enPassantTarget(Square square) {
            this.enPassantTarget = square;
            return this;
        }

        public Position build() {
            return new Position(this);
        }
    }
}
