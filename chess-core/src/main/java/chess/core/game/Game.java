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
 * (consola, GUI, tests).
 *
 * <p>Responsabilidades: llevar la posición actual, alternar los turnos, validar cada intento
 * de movimiento contra las reglas, registrar el historial, decidir si la partida terminó y
 * avisar a los {@link GameListener}.
 *
 * <p>Todas sus dependencias llegan por constructor: el generador de movimientos y las
 * condiciones de fin de partida. No sabe nada de piezas concretas; eso lo aportan los
 * {@code PieceType} de la {@link Position}.
 */
public final class Game {

    private final MoveGenerator generator;
    private final List<EndCondition> endConditions;
    private final List<GameListener> listeners = new ArrayList<>();
    private final List<Move> history = new ArrayList<>();

    private Position position;
    private List<Move> legalMoves;
    private GameResult result;

    public Game(Position initial, MoveGenerator generator, List<EndCondition> endConditions) {
        this.position = initial;
        this.generator = generator;
        this.endConditions = List.copyOf(endConditions);
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
        return legalMoves().stream().filter(move -> move.from().equals(from)).toList();
    }

    /** Movimientos jugados hasta ahora, en orden. */
    public List<Move> history() {
        return Collections.unmodifiableList(history);
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    // ---- jugar ---------------------------------------------------------------------------

    /** Juega un movimiento ya construido; falla si no es legal en la posición actual. */
    public void play(Move move) {
        if (isOver()) {
            throw new IllegalMoveException("La partida ya terminó: " + result.reason());
        }
        if (!legalMoves.contains(move)) {
            throw new IllegalMoveException("Movimiento no permitido: " + move.notation());
        }
        apply(move);
    }

    /** Juega el movimiento de {@code from} a {@code to}. Si es una promoción hay que elegir la pieza. */
    public Move play(Square from, Square to) {
        return playResolved(from, to, null);
    }

    /** Juega una promoción: {@code promotionSymbol} es la letra de la pieza elegida (p. ej. 'q'). */
    public Move play(Square from, Square to, char promotionSymbol) {
        return playResolved(from, to, promotionSymbol);
    }

    private Move playResolved(Square from, Square to, Character promotionSymbol) {
        if (isOver()) {
            throw new IllegalMoveException("La partida ya terminó: " + result.reason());
        }
        for (Square square : List.of(from, to)) {
            if (!position.geometry().contains(square)) {
                throw new IllegalMoveException("La casilla " + square + " no existe en un tablero de "
                        + position.geometry());
            }
        }
        Piece piece = position.pieceAt(from)
                .orElseThrow(() -> new IllegalMoveException("No hay ninguna pieza en " + from));
        if (piece.color() != turn()) {
            throw new IllegalMoveException("No es tu turno: la pieza en " + from + " es de otro color");
        }

        List<Move> candidates = legalMoves.stream()
                .filter(move -> move.from().equals(from) && move.to().equals(to))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalMoveException(explainIllegal(piece, from, to));
        }

        Move chosen;
        if (promotionSymbol != null) {
            chosen = candidates.stream()
                    .filter(m -> m.promotion().isPresent()
                            && Character.toUpperCase(m.promotion().get().symbol())
                            == Character.toUpperCase(promotionSymbol))
                    .findFirst()
                    .orElseThrow(() -> new IllegalMoveException(
                            "No se puede promocionar a '" + promotionSymbol + "' en ese movimiento"));
        } else if (candidates.size() == 1) {
            chosen = candidates.get(0);
        } else {
            throw new IllegalMoveException("Indicá la pieza de promoción (por ejemplo: "
                    + from + to + "q)");
        }
        apply(chosen);
        return chosen;
    }

    private String explainIllegal(Piece piece, Square from, Square to) {
        boolean allowedByPiece = generator.pseudoLegalMoves(position).stream()
                .anyMatch(move -> move.from().equals(from) && move.to().equals(to));
        if (allowedByPiece) {
            return "Movimiento ilegal: dejaría a tu rey en jaque";
        }
        return piece.type().name() + " no puede moverse de " + from + " a " + to;
    }

    private void apply(Move move) {
        position = position.play(move);
        history.add(move);
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

    private void refresh() {
        legalMoves = generator.legalMoves(position);
        result = null;
        for (EndCondition condition : endConditions) {
            Optional<GameResult> outcome = condition.evaluate(position, legalMoves);
            if (outcome.isPresent()) {
                result = outcome.get();
                break;
            }
        }
    }
}
