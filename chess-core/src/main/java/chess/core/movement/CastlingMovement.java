package chess.core.movement;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.MovementRule;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.move.CastlingMove;

import java.util.List;
import java.util.function.Consumer;

/**
 * Regla adicional del rey: el enroque. Se combina con los pasos del rey mediante
 * {@link CompositeMovement}; no es parte de "cómo se mueve" el rey sino una regla extra.
 *
 * <p>Condiciones (todas se expresan con bitboards):
 * <ol>
 *   <li>El rey y la torre involucrados nunca se movieron: ambas casillas están en {@code unmoved}.</li>
 *   <li>Hay una torre de ese bando en su casilla.</li>
 *   <li>Las casillas entre el rey y la torre, y las de destino, están libres.</li>
 *   <li>El rey no está en jaque, no pasa por una casilla atacada ni cae en una.</li>
 * </ol>
 * El enroque no ataca nada, por lo que no influye en la detección de jaque.
 */
public final class CastlingMovement implements MovementRule {

    private final PieceType rook;
    private final List<CastlingSpec> specs;

    public CastlingMovement(PieceType rook, List<CastlingSpec> specs) {
        this.rook = rook;
        this.specs = List.copyOf(specs);
    }

    @Override
    public Bitboard attacks(Position position, Square from, Color color) {
        return Bitboard.EMPTY;
    }

    @Override
    public void generate(Position position, Square from, Color color, Consumer<Move> out) {
        BoardGeometry geometry = position.geometry();
        for (CastlingSpec spec : specs) {
            if (spec.color() != color || !spec.kingFrom().equals(from)) {
                continue;
            }
            Bitboard origins = geometry.bit(spec.kingFrom()).or(geometry.bit(spec.rookFrom()));
            boolean bothUnmoved = position.unmoved().and(origins).equals(origins);
            boolean rookPresent = position.pieces(color, rook).has(geometry.index(spec.rookFrom()));
            if (!bothUnmoved || !rookPresent) {
                continue;
            }

            Bitboard destinations = geometry.bit(spec.kingTo()).or(geometry.bit(spec.rookTo()));
            Bitboard mustBeEmpty = geometry.between(spec.kingFrom(), spec.rookFrom())
                    .or(destinations)
                    .andNot(origins);
            if (mustBeEmpty.intersects(position.occupied())) {
                continue;
            }

            Bitboard kingPath = geometry.between(spec.kingFrom(), spec.kingTo())
                    .or(geometry.bit(spec.kingFrom()))
                    .or(geometry.bit(spec.kingTo()));
            if (kingPath.intersects(position.attackedBy(color.opposite()))) {
                continue;
            }

            out.accept(new CastlingMove(spec.kingFrom(), spec.kingTo(), spec.rookFrom(), spec.rookTo()));
        }
    }
}
