package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.movement.PawnMovement;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Estado completo e <b>inmutable</b> de una partida en un instante:
 * dónde está cada pieza, a quién le toca, qué piezas nunca se movieron
 * (base del derecho a enrocar), si hay una casilla de captura al paso
 * disponible y cuántos medios movimientos transcurrieron desde la
 * última captura o movimiento de peón.
 *
 * <p>Las piezas se guardan como bitboards: uno por color y uno por tipo
 * de pieza. Para saber qué hay en una casilla se cruzan (AND) esos conjuntos.
 *
 * <p>Que sea inmutable permite probar un movimiento mediante {@link #play}
 * sin tener que deshacerlo después, compartir posiciones entre hilos sin
 * riesgo y guardar el historial completo de la partida.
 *
 * <p>Los cambios se arman mediante un {@link Builder}.
 */
public final class Position {

    private final BoardGeometry geometry;
    private final Map<PieceType, Bitboard> byType;
    private final Map<Color, Bitboard> byColor;
    private final Bitboard occupied;
    private final Color sideToMove;
    private final Bitboard unmoved;
    private final Square enPassantTarget;

    /**
     * Cantidad de medios movimientos transcurridos desde la última
     * captura o movimiento de peón.
     *
     * <p>Cada movimiento de uno de los jugadores cuenta como un medio
     * movimiento. Este valor se utilizará para evaluar la regla de los
     * cincuenta movimientos.
     */
    private final int halfmoveClock;

    // Caché perezosa del mapa de ataques de cada bando.
    // La posición es inmutable, así que nunca se invalida.
    // Si dos hilos la calculan a la vez obtienen el mismo valor.
    private volatile Bitboard attackedByWhite;
    private volatile Bitboard attackedByBlack;

    private Position(Builder builder) {
        this.geometry = builder.geometry;

        Map<PieceType, Bitboard> types =
                new LinkedHashMap<>();

        builder.byType.forEach((type, board) -> {
            if (!board.isEmpty()) {
                types.put(type, board);
            }
        });

        this.byType =
                Collections.unmodifiableMap(types);

        Map<Color, Bitboard> colors =
                new EnumMap<>(Color.class);

        colors.put(
                Color.WHITE,
                builder.byColor.get(Color.WHITE)
        );

        colors.put(
                Color.BLACK,
                builder.byColor.get(Color.BLACK)
        );

        this.byColor =
                Collections.unmodifiableMap(colors);

        this.occupied =
                colors.get(Color.WHITE)
                        .or(colors.get(Color.BLACK));

        this.sideToMove =
                builder.sideToMove;

        this.unmoved =
                builder.unmoved;

        this.enPassantTarget =
                builder.enPassantTarget;

        this.halfmoveClock =
                builder.halfmoveClock;
    }

    /**
     * Crea un tablero vacío, con el turno de las blancas,
     * listo para ir colocando piezas.
     */
    public static Builder builder(BoardGeometry geometry) {
        return new Builder(geometry);
    }

    /**
     * Devuelve una copia modificable de esta posición.
     */
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

    /**
     * Devuelve todas las casillas ocupadas.
     */
    public Bitboard occupied() {
        return occupied;
    }

    public Bitboard occupiedBy(Color color) {
        return byColor.get(color);
    }

    /**
     * Devuelve las casillas de las piezas del tipo indicado,
     * independientemente de su color.
     */
    public Bitboard pieces(PieceType type) {
        return byType.getOrDefault(
                type,
                Bitboard.EMPTY
        );
    }

    /**
     * Devuelve las casillas de las piezas del tipo y color indicados.
     */
    public Bitboard pieces(
            Color color,
            PieceType type
    ) {
        return pieces(type)
                .and(byColor.get(color));
    }

    /**
     * Devuelve los tipos de pieza presentes actualmente en el tablero.
     */
    public Set<PieceType> pieceTypes() {
        return byType.keySet();
    }

    public Optional<Piece> pieceAt(Square square) {
        int index =
                geometry.index(square);

        Color color;

        if (byColor.get(Color.WHITE).has(index)) {
            color = Color.WHITE;
        } else if (byColor.get(Color.BLACK).has(index)) {
            color = Color.BLACK;
        } else {
            return Optional.empty();
        }

        for (Map.Entry<PieceType, Bitboard> entry
                : byType.entrySet()) {

            if (entry.getValue().has(index)) {
                return Optional.of(
                        new Piece(
                                entry.getKey(),
                                color
                        )
                );
            }
        }

        throw new IllegalStateException(
                "Inconsistencia: casilla ocupada sin tipo de pieza en "
                        + square
        );
    }

    /**
     * Devuelve las casillas cuyas piezas todavía no se movieron,
     * fueron capturadas o reemplazadas.
     */
    public Bitboard unmoved() {
        return unmoved;
    }

    /**
     * Devuelve la casilla situada detrás del peón que acaba de avanzar
     * dos pasos, si existe una captura al paso disponible.
     */
    public Optional<Square> enPassantTarget() {
        return Optional.ofNullable(
                enPassantTarget
        );
    }

    /**
     * Devuelve la cantidad de medios movimientos transcurridos desde
     * la última captura o movimiento de peón.
     */
    public int halfmoveClock() {
        return halfmoveClock;
    }

    // ---- ataques y jaque -----------------------------------------------------------------

    /**
     * Devuelve la unión de las casillas controladas por todas las
     * piezas del color indicado.
     *
     * <p>El resultado se calcula una sola vez por posición y luego
     * se reutiliza.
     */
    public Bitboard attackedBy(Color color) {
        Bitboard cached =
                color == Color.WHITE
                        ? attackedByWhite
                        : attackedByBlack;

        if (cached != null) {
            return cached;
        }

        Bitboard attacked =
                Bitboard.EMPTY;

        for (Map.Entry<PieceType, Bitboard> entry
                : byType.entrySet()) {

            MovementRule rule =
                    entry.getKey().movement();

            Bitboard piecesOfColor =
                    entry.getValue()
                            .and(byColor.get(color));

            for (Square from
                    : geometry.squares(piecesOfColor)) {

                attacked = attacked.or(
                        rule.attacks(
                                this,
                                from,
                                color
                        )
                );
            }
        }

        if (color == Color.WHITE) {
            attackedByWhite = attacked;
        } else {
            attackedByBlack = attacked;
        }

        return attacked;
    }

    /**
     * Devuelve {@code true} si alguna pieza real del color indicado
     * está siendo atacada.
     */
    public boolean isInCheck(Color color) {
        Bitboard royal =
                Bitboard.EMPTY;

        for (PieceType type : byType.keySet()) {
            if (type.royal()) {
                royal = royal.or(
                        pieces(color, type)
                );
            }
        }

        return !royal.isEmpty()
                && royal.intersects(
                attackedBy(color.opposite())
        );
    }

    // ---- transición ----------------------------------------------------------------------

    /**
     * Devuelve la posición resultante de aplicar el movimiento indicado.
     *
     * <p>Este método no valida que el movimiento sea legal. De esa
     * responsabilidad se ocupan el generador de movimientos y {@code Game}.
     *
     * <p>El contador de medios movimientos aumenta con cada movimiento
     * que no sea realizado por un peón y que no produzca una captura.
     * Un movimiento de peón o una captura reinician el contador a cero.
     */
    public Position play(Move move) {
        Piece movingPiece = pieceAt(move.from())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No hay pieza en " + move.from()
                        )
                );

        /*
         * Se identifica al peón por su estrategia de movimiento,
         * no por una pieza concreta de StandardPieces.
         *
         * De este modo, Position continúa siendo independiente
         * de la configuración concreta del ajedrez estándar.
         */
        boolean pawnMove =
                movingPiece.type().movement()
                        instanceof PawnMovement;

        /*
         * En una captura común, la pieza capturada ocupa la casilla
         * de destino antes de ejecutar el movimiento.
         *
         * La captura al paso tiene el destino vacío, pero igualmente
         * reinicia el contador porque siempre es un movimiento de peón.
         */
        boolean capture =
                pieceAt(move.to()).isPresent();

        Builder builder =
                toBuilder();

        // El derecho de captura al paso dura un solo turno.
        builder.enPassantTarget(null);

        move.execute(builder);

        if (pawnMove || capture) {
            builder.halfmoveClock(0);
        } else {
            builder.halfmoveClock(
                    halfmoveClock + 1
            );
        }

        builder.sideToMove(
                sideToMove.opposite()
        );

        return builder.build();
    }

    // ---- igualdad ------------------------------------------------------------------------

    /**
     * Compara la identidad ajedrecística utilizada actualmente para
     * determinar si dos posiciones son equivalentes.
     *
     * <p>El contador de medios movimientos no forma parte de esta igualdad.
     * De este modo, una misma disposición de piezas alcanzada con contadores
     * diferentes puede seguir contando como la misma posición para la regla
     * de triple repetición.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Position other)) {
            return false;
        }

        return geometry.equals(other.geometry)
                && sideToMove == other.sideToMove
                && byType.equals(other.byType)
                && byColor.equals(other.byColor)
                && unmoved.equals(other.unmoved)
                && Objects.equals(
                enPassantTarget,
                other.enPassantTarget
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                geometry,
                sideToMove,
                byType,
                byColor,
                unmoved,
                enPassantTarget
        );
    }

    // ---- constructor mutable -------------------------------------------------------------

    /**
     * Versión mutable y temporal de una {@link Position}.
     *
     * <p>Es el objeto que reciben los {@link Move} para aplicar sus
     * modificaciones. Al finalizar la construcción se llama a
     * {@link #build()} para obtener una nueva posición inmutable.
     */
    public static final class Builder {

        private final BoardGeometry geometry;

        private final Map<PieceType, Bitboard> byType =
                new LinkedHashMap<>();

        private final Map<Color, Bitboard> byColor =
                new EnumMap<>(Color.class);

        private Color sideToMove =
                Color.WHITE;

        private Bitboard unmoved =
                Bitboard.EMPTY;

        private Square enPassantTarget;

        /**
         * Un builder nuevo representa una posición que todavía no tiene
         * medios movimientos acumulados.
         */
        private int halfmoveClock;

        private Builder(BoardGeometry geometry) {
            this.geometry =
                    Objects.requireNonNull(
                            geometry,
                            "geometry"
                    );

            byColor.put(
                    Color.WHITE,
                    Bitboard.EMPTY
            );

            byColor.put(
                    Color.BLACK,
                    Bitboard.EMPTY
            );
        }

        private Builder(Position source) {
            this.geometry =
                    source.geometry;

            this.byType.putAll(
                    source.byType
            );

            this.byColor.putAll(
                    source.byColor
            );

            this.sideToMove =
                    source.sideToMove;

            this.unmoved =
                    source.unmoved;

            this.enPassantTarget =
                    source.enPassantTarget;

            this.halfmoveClock =
                    source.halfmoveClock;
        }

        public BoardGeometry geometry() {
            return geometry;
        }

        public Optional<Piece> pieceAt(Square square) {
            int index =
                    geometry.index(square);

            Color color;

            if (byColor.get(Color.WHITE).has(index)) {
                color = Color.WHITE;
            } else if (byColor.get(Color.BLACK).has(index)) {
                color = Color.BLACK;
            } else {
                return Optional.empty();
            }

            for (Map.Entry<PieceType, Bitboard> entry
                    : byType.entrySet()) {

                if (entry.getValue().has(index)) {
                    return Optional.of(
                            new Piece(
                                    entry.getKey(),
                                    color
                            )
                    );
                }
            }

            throw new IllegalStateException(
                    "Inconsistencia: casilla ocupada sin tipo de pieza en "
                            + square
            );
        }

        /**
         * Coloca una pieza, reemplazando lo que hubiera anteriormente,
         * y marca la casilla como ya movida.
         */
        public Builder place(
                Square square,
                Piece piece
        ) {
            remove(square);

            int index =
                    geometry.index(square);

            byType.merge(
                    piece.type(),
                    Bitboard.ofBit(index),
                    Bitboard::or
            );

            byColor.merge(
                    piece.color(),
                    Bitboard.ofBit(index),
                    Bitboard::or
            );

            return this;
        }

        /**
         * Quita la pieza de la casilla, si existe, y marca la casilla
         * como ya movida.
         */
        public Builder remove(Square square) {
            Optional<Piece> existing =
                    pieceAt(square);

            int index =
                    geometry.index(square);

            if (existing.isPresent()) {
                Piece piece =
                        existing.get();

                byType.computeIfPresent(
                        piece.type(),
                        (type, board) ->
                                board.without(index)
                );

                byColor.computeIfPresent(
                        piece.color(),
                        (color, board) ->
                                board.without(index)
                );
            }

            unmoved =
                    unmoved.without(index);

            return this;
        }

        /**
         * Mueve la pieza de {@code from} a {@code to}. Si en la casilla
         * de destino había otra pieza, la pieza queda capturada.
         */
        public Builder relocate(
                Square from,
                Square to
        ) {
            Piece piece = pieceAt(from)
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "No hay pieza en " + from
                                    )
                    );

            remove(from);
            place(to, piece);

            return this;
        }

        public Builder sideToMove(Color color) {
            this.sideToMove =
                    Objects.requireNonNull(
                            color,
                            "color"
                    );

            return this;
        }

        /**
         * Define qué casillas contienen piezas que nunca se movieron.
         * Este estado sirve como base para los derechos de enroque.
         */
        public Builder unmoved(Bitboard squares) {
            this.unmoved =
                    Objects.requireNonNull(
                            squares,
                            "squares"
                    );

            return this;
        }

        /**
         * Define la casilla de captura al paso disponible para el próximo
         * turno, o {@code null} si no existe.
         */
        public Builder enPassantTarget(Square square) {
            this.enPassantTarget =
                    square;

            return this;
        }

        /**
         * Define la cantidad de medios movimientos transcurridos desde
         * la última captura o movimiento de peón.
         *
         * @throws IllegalArgumentException si el valor es negativo
         */
        public Builder halfmoveClock(int halfmoveClock) {
            if (halfmoveClock < 0) {
                throw new IllegalArgumentException(
                        "El contador de medios movimientos no puede ser negativo"
                );
            }

            this.halfmoveClock =
                    halfmoveClock;

            return this;
        }

        public Position build() {
            return new Position(this);
        }
    }
}