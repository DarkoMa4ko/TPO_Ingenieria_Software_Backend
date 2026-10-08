package chess.core.movement;

import chess.core.board.Bitboard;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.MovementRule;
import chess.core.model.Position;

import java.util.List;
import java.util.function.Consumer;

/**
 * Patrón Composite: una regla formada por otras reglas. Sus ataques son la unión de los de las
 * partes y sus movimientos son los de todas ellas.
 *
 * <p>Así se arman piezas por composición, sin herencia:
 * <ul>
 *   <li>Dama = torre + alfil.</li>
 *   <li>Rey = pasos de rey + enroque.</li>
 *   <li>Arzobispo (variante Capablanca) = alfil + caballo.</li>
 * </ul>
 * Si dos partes producen el mismo movimiento, el generador lo deduplica.
 */
public final class CompositeMovement implements MovementRule {

    private final List<MovementRule> parts;

    public CompositeMovement(MovementRule... parts) {
        this.parts = List.of(parts);
    }

    @Override
    public Bitboard attacks(Position position, Square from, Color color) {
        Bitboard attacked = Bitboard.EMPTY;
        for (MovementRule part : parts) {
            attacked = attacked.or(part.attacks(position, from, color));
        }
        return attacked;
    }

    @Override
    public void generate(Position position, Square from, Color color, Consumer<Move> out) {
        for (MovementRule part : parts) {
            part.generate(position, from, color, out);
        }
    }
}
