package chess.core.game;

import chess.core.TestSupport;
import chess.core.model.Move;
import chess.core.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoyalCaptureTest {

    @Test
    void pseudoLegalMovesMayAttackTheEnemyKingSquare() {
        Position position =
                TestSupport.position(
                        "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1"
                );

        LegalMoveGenerator generator =
                new LegalMoveGenerator();

        List<Move> pseudo =
                generator.pseudoLegalMoves(
                        position
                );

        assertTrue(
                pseudo.stream()
                        .anyMatch(move ->
                                move.from().equals(
                                        TestSupport.sq("e2")
                                )
                                        && move.to().equals(
                                        TestSupport.sq("e8")
                                )
                        )
        );
    }

    @Test
    void legalMovesNeverCaptureTheEnemyKing() {
        Position position =
                TestSupport.position(
                        "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1"
                );

        LegalMoveGenerator generator =
                new LegalMoveGenerator();

        List<Move> legal =
                generator.legalMoves(
                        position
                );

        assertFalse(
                legal.stream()
                        .anyMatch(move ->
                                move.to().equals(
                                        TestSupport.sq("e8")
                                )
                        )
        );
    }
}