package chess.core.game;

import chess.core.board.Bitboard;
import chess.core.board.Square;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.PieceType;
import chess.core.model.Position;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generador estándar: pide a cada pieza del jugador en turno que genere sus movimientos
 * (cada una con su {@code MovementRule}) y descarta los que dejan al propio rey en jaque.
 *
 * <p>El filtro de jaque se hace "jugando" el movimiento sobre una posición inmutable y
 * preguntando si el rey quedó atacado. No hace falta deshacer nada.
 */
public final class LegalMoveGenerator implements MoveGenerator {

    @Override
    public List<Move> pseudoLegalMoves(Position position) {
        Color mover = position.sideToMove();
        Set<Move> moves = new LinkedHashSet<>(); // deduplica si dos reglas de una pieza coinciden
        for (PieceType type : position.pieceTypes()) {
            Bitboard pieces = position.pieces(mover, type);
            for (Square from : position.geometry().squares(pieces)) {
                type.movement().generate(position, from, mover, moves::add);
            }
        }
        return List.copyOf(moves);
    }

    @Override
    public List<Move> legalMoves(Position position) {
        Color mover = position.sideToMove();
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegalMoves(position)) {
            if (!position.play(move).isInCheck(mover)) {
                legal.add(move);
            }
        }
        return List.copyOf(legal);
    }
}
