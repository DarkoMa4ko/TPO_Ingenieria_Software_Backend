package chess.core.standard;

import chess.core.model.Color;
import chess.core.model.Position;

public final class StandardPositionValidator {

    private StandardPositionValidator() {
    }

    public static void validate(Position position) {
        requireExactlyOneKing(
                position,
                Color.WHITE
        );

        requireExactlyOneKing(
                position,
                Color.BLACK
        );

        boolean whiteInCheck =
                position.isInCheck(
                        Color.WHITE
                );

        boolean blackInCheck =
                position.isInCheck(
                        Color.BLACK
                );

        /*
         * Los dos reyes no pueden estar simultáneamente en jaque
         * en una posición legal de ajedrez estándar.
         */
        if (whiteInCheck && blackInCheck) {
            throw new IllegalArgumentException(
                    "Posición inválida: ambos reyes están en jaque"
            );
        }

        /*
         * El jugador que acaba de realizar el movimiento es el
         * contrario al que tiene actualmente el turno.
         *
         * No puede haber terminado su jugada dejando su propio
         * rey en jaque.
         */
        Color sideThatJustMoved =
                position.sideToMove()
                        .opposite();

        if (position.isInCheck(
                sideThatJustMoved
        )) {
            throw new IllegalArgumentException(
                    "Posición inválida: el jugador que acaba de mover dejó su rey en jaque"
            );
        }
    }

    private static void requireExactlyOneKing(
            Position position,
            Color color
    ) {
        int kings =
                position.pieces(
                                color,
                                StandardPieces.KING
                        )
                        .cardinality();

        if (kings != 1) {
            String colorName =
                    color == Color.WHITE
                            ? "blanco"
                            : "negro";

            throw new IllegalArgumentException(
                    "La posición debe contener exactamente un rey "
                            + colorName
            );
        }
    }
}
