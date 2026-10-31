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

public final class LegalMoveGenerator implements MoveGenerator {

    @Override
    public List<Move> pseudoLegalMoves(Position position) {
        Color mover =
                position.sideToMove();

        Set<Move> moves =
                new LinkedHashSet<>();

        for (PieceType type : position.pieceTypes()) {
            Bitboard pieces =
                    position.pieces(
                            mover,
                            type
                    );

            for (Square from
                    : position.geometry().squares(pieces)) {

                type.movement().generate(
                        position,
                        from,
                        mover,
                        moves::add
                );
            }
        }

        return List.copyOf(moves);
    }

    @Override
    public List<Move> legalMoves(Position position) {
        Color mover =
                position.sideToMove();

        List<Move> legal =
                new ArrayList<>();

        for (Move move
                : pseudoLegalMoves(position)) {

            /*
             * En ajedrez el rey nunca se captura.
             *
             * El jaque mate termina la partida cuando el rey queda
             * amenazado y no existen respuestas legales.
             */
            if (capturesRoyalPiece(
                    position,
                    move
            )) {
                continue;
            }

            Position resulting =
                    position.play(move);

            if (!resulting.isInCheck(mover)) {
                legal.add(move);
            }
        }

        return List.copyOf(legal);
    }

    /**
     * Indica si el movimiento terminaría capturando una pieza
     * marcada como royal.
     *
     * <p>No se comprueba específicamente KING para mantener al
     * generador independiente de StandardPieces y permitir otras
     * configuraciones del motor.
     */
    private static boolean capturesRoyalPiece(
            Position position,
            Move move
    ) {
        return position.pieceAt(
                        move.to()
                )
                .map(piece ->
                        piece.type().royal()
                )
                .orElse(false);
    }
}