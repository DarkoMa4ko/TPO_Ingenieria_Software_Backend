package chess.core.standard;

import chess.core.board.Direction;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.PieceType;
import chess.core.movement.CastlingMovement;
import chess.core.movement.CastlingSpec;
import chess.core.movement.CompositeMovement;
import chess.core.movement.LeapingMovement;
import chess.core.movement.PawnMovement;
import chess.core.movement.SlidingMovement;

import java.util.List;

/**
 * Las seis piezas del ajedrez clásico, armadas <b>por composición</b> de reglas de movimiento.
 * No hay una clase por pieza ni una jerarquía de herencia: cada pieza es datos + una estrategia.
 *
 * <pre>
 *   Torre   = deslizar en las 4 direcciones ortogonales
 *   Alfil   = deslizar en las 4 diagonales
 *   Reina   = Torre + Alfil
 *   Caballo = saltar en L
 *   Rey     = saltar 1 casilla en las 8 direcciones + enroque
 *   Peón    = regla propia (avanza recto, captura en diagonal, al paso, promoción)
 * </pre>
 *
 * Para sumar una pieza basta con crear otro {@code PieceType}; no hay que modificar nada de acá.
 */
public final class StandardPieces {

    private StandardPieces() {
    }

    public static final PieceType ROOK = new PieceType("Torre", 'R',
            new SlidingMovement(Direction.ORTHOGONAL));

    public static final PieceType BISHOP = new PieceType("Alfil", 'B',
            new SlidingMovement(Direction.DIAGONAL));

    public static final PieceType KNIGHT = new PieceType("Caballo", 'N',
            new LeapingMovement(Direction.KNIGHT_JUMPS));

    public static final PieceType QUEEN = new PieceType("Reina", 'Q',
            new CompositeMovement(ROOK.movement(), BISHOP.movement()));

    /** Los cuatro enroques del ajedrez clásico, declarados como datos. */
    public static final List<CastlingSpec> CASTLING = List.of(
            new CastlingSpec(Color.WHITE, sq("e1"), sq("g1"), sq("h1"), sq("f1")),
            new CastlingSpec(Color.WHITE, sq("e1"), sq("c1"), sq("a1"), sq("d1")),
            new CastlingSpec(Color.BLACK, sq("e8"), sq("g8"), sq("h8"), sq("f8")),
            new CastlingSpec(Color.BLACK, sq("e8"), sq("c8"), sq("a8"), sq("d8")));

    public static final PieceType KING = new PieceType("Rey", 'K',
            new CompositeMovement(
                    new LeapingMovement(Direction.ALL),
                    new CastlingMovement(ROOK, CASTLING)),
            true);

    public static final PieceType PAWN = new PieceType("Peón", 'P',
            new PawnMovement(List.of(QUEEN, ROOK, BISHOP, KNIGHT)));

    /** Todas las piezas estándar. */
    public static final List<PieceType> ALL = List.of(PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING);

    private static Square sq(String text) {
        return Square.parse(text);
    }
}
