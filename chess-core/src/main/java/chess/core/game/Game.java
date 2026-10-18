package chess.core.game;

import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Piece;
import chess.core.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Una partida en curso: es el punto de entrada del núcleo para cualquier adaptador
 * (consola, GUI, API, tests).
 *
 * <p>Responsabilidades: llevar la posición actual, alternar los turnos, validar cada intento
 * de movimiento contra las reglas, registrar el historial de movimientos y posiciones,
 * evaluar las condiciones de fin de partida y avisar a los {@link GameListener}.
 *
 * <p>El historial de posiciones forma parte del estado de la partida porque algunas reglas
 * de ajedrez, como las tablas por repetición, necesitan conocer posiciones anteriores y no
 * únicamente la posición actual.
 *
 * <p>Todas sus dependencias llegan por constructor: el generador de movimientos y las
 * condiciones de fin de partida. No sabe nada de piezas concretas; eso lo aportan los
 * {@code PieceType} de la {@link Position}. Tampoco conoce detalles de infraestructura
 * como persistencia, base de datos, HTTP o interfaz de usuario.
 */
public final class Game {

    private final MoveGenerator generator;
    private final List<EndCondition> endConditions;
    private final List<GameListener> listeners = new ArrayList<>();

    /**
     * Movimientos ejecutados durante la partida, en el orden en que fueron jugados.
     */
    private final List<Move> history = new ArrayList<>();

    /**
     * Posiciones alcanzadas durante la partida.
     *
     * <p>Incluye la posición inicial como primer elemento y, posteriormente,
     * la posición resultante de cada movimiento ejecutado.
     */
    private final List<Position> positionHistory = new ArrayList<>();

    private Position position;
    private List<Move> legalMoves;
    private GameResult result;

    public Game(
            Position initial,
            MoveGenerator generator,
            List<EndCondition> endConditions
    ) {
        this.position = initial;
        this.generator = generator;
        this.endConditions = List.copyOf(endConditions);

        // La posición inicial también forma parte del historial.
        this.positionHistory.add(initial);

        refresh();
    }

    /**
     * Reconstruye una partida desde una posición actual y un historial
     * de posiciones previamente alcanzadas.
     *
     * <p>Este constructor permite restaurar el contexto necesario para reglas
     * que dependen de la evolución de la partida, como las tablas por repetición.
     *
     * <p>Game sigue trabajando únicamente con conceptos del dominio.
     * No conoce cómo ni dónde fue almacenado el historial.
     *
     * @param current posición actual de la partida
     * @param generator generador de movimientos de la partida
     * @param endConditions condiciones de finalización
     * @param positionHistory posiciones alcanzadas durante la partida,
     *                        incluyendo la posición inicial y la actual
     */
    public Game(
            Position current,
            MoveGenerator generator,
            List<EndCondition> endConditions,
            List<Position> positionHistory
    ) {
        if (positionHistory.isEmpty()) {
            throw new IllegalArgumentException(
                    "El historial de posiciones no puede estar vacío"
            );
        }

        this.position = current;
        this.generator = generator;
        this.endConditions = List.copyOf(endConditions);

        this.positionHistory.addAll(positionHistory);

        refresh();
    }

    // ---- consultas -----------------------------------------------------------------------

    public Position position() {
        return position;
    }

    /** Color al que le toca mover. */
    public Color turn() {
        return position.sideToMove();
    }

    /** {@code true} si el jugador en turno tiene su rey en jaque. */
    public boolean isInCheck() {
        return position.isInCheck(position.sideToMove());
    }

    public boolean isOver() {
        return result != null;
    }

    public Optional<GameResult> result() {
        return Optional.ofNullable(result);
    }

    /** Movimientos legales del jugador en turno (vacío si la partida terminó). */
    public List<Move> legalMoves() {
        return isOver() ? List.of() : legalMoves;
    }

    public List<Move> legalMovesFrom(Square from) {
        return legalMoves()
                .stream()
                .filter(move -> move.from().equals(from))
                .toList();
    }

    /** Movimientos jugados hasta ahora, en orden. */
    public List<Move> history() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Posiciones alcanzadas durante la partida, incluyendo la posición inicial.
     *
     * <p>La lista devuelta es de solo lectura para evitar que código externo
     * modifique el historial interno de la partida.
     */
    public List<Position> positionHistory() {
        return Collections.unmodifiableList(positionHistory);
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    // ---- jugar ---------------------------------------------------------------------------

    /** Juega un movimiento ya construido; falla si no es legal en la posición actual. */
    public void play(Move move) {
        if (isOver()) {
            throw new IllegalMoveException(
                    "La partida ya terminó: " + result.reason()
            );
        }

        if (!legalMoves.contains(move)) {
            throw new IllegalMoveException(
                    "Movimiento no permitido: " + move.notation()
            );
        }

        apply(move);
    }

    /**
     * Juega el movimiento de {@code from} a {@code to}.
     * Si es una promoción hay que elegir la pieza.
     */
    public Move play(Square from, Square to) {
        return playResolved(from, to, null);
    }

    /**
     * Juega una promoción:
     * {@code promotionSymbol} es la letra de la pieza elegida (p. ej. 'q').
     */
    public Move play(
            Square from,
            Square to,
            char promotionSymbol
    ) {
        return playResolved(from, to, promotionSymbol);
    }

    private Move playResolved(
            Square from,
            Square to,
            Character promotionSymbol
    ) {
        if (isOver()) {
            throw new IllegalMoveException(
                    "La partida ya terminó: " + result.reason()
            );
        }

        for (Square square : List.of(from, to)) {
            if (!position.geometry().contains(square)) {
                throw new IllegalMoveException(
                        "La casilla " + square
                                + " no existe en un tablero de "
                                + position.geometry()
                );
            }
        }

        Piece piece = position.pieceAt(from)
                .orElseThrow(
                        () -> new IllegalMoveException(
                                "No hay ninguna pieza en " + from
                        )
                );

        if (piece.color() != turn()) {
            throw new IllegalMoveException(
                    "No es tu turno: la pieza en "
                            + from
                            + " es de otro color"
            );
        }

        List<Move> candidates = legalMoves
                .stream()
                .filter(
                        move -> move.from().equals(from)
                                && move.to().equals(to)
                )
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalMoveException(
                    explainIllegal(piece, from, to)
            );
        }

        Move chosen;

        if (promotionSymbol != null) {
            chosen = candidates
                    .stream()
                    .filter(
                            move -> move.promotion().isPresent()
                                    && Character.toUpperCase(
                                    move.promotion().get().symbol()
                            )
                                    == Character.toUpperCase(
                                    promotionSymbol
                            )
                    )
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalMoveException(
                                    "No se puede promocionar a '"
                                            + promotionSymbol
                                            + "' en ese movimiento"
                            )
                    );

        } else if (candidates.size() == 1) {
            chosen = candidates.get(0);

        } else {
            throw new IllegalMoveException(
                    "Indicá la pieza de promoción (por ejemplo: "
                            + from
                            + to
                            + "q)"
            );
        }

        apply(chosen);

        return chosen;
    }

    private String explainIllegal(
            Piece piece,
            Square from,
            Square to
    ) {
        boolean allowedByPiece = generator
                .pseudoLegalMoves(position)
                .stream()
                .anyMatch(
                        move -> move.from().equals(from)
                                && move.to().equals(to)
                );

        if (allowedByPiece) {
            return "Movimiento ilegal: dejaría a tu rey en jaque";
        }

        return piece.type().name()
                + " no puede moverse de "
                + from
                + " a "
                + to;
    }

    /**
     * Aplica un movimiento legal y actualiza el estado interno de la partida.
     *
     * <p>Además del historial de movimientos, se registra la nueva posición
     * resultante. Esto permite que condiciones de fin que dependen de la
     * evolución de la partida puedan consultar posiciones anteriores.
     */
    private void apply(Move move) {
        position = position.play(move);

        history.add(move);
        positionHistory.add(position);

        refresh();

        for (GameListener listener : List.copyOf(listeners)) {
            listener.onMovePlayed(this, move);
        }

        if (result != null) {
            for (GameListener listener : List.copyOf(listeners)) {
                listener.onGameOver(this, result);
            }
        }
    }

    /**
     * Recalcula los movimientos legales y evalúa las condiciones de fin
     * de partida sobre el contexto actualizado.
     */
    private void refresh() {
        legalMoves = generator.legalMoves(position);
        result = null;

        GameContext context = new GameContext(
                position,
                legalMoves,
                positionHistory
        );

        for (EndCondition condition : endConditions) {
            Optional<GameResult> outcome =
                    condition.evaluate(context);

            if (outcome.isPresent()) {
                result = outcome.get();
                break;
            }
        }
    }
}