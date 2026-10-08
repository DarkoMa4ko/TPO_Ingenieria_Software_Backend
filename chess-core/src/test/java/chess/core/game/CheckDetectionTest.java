package chess.core.game;

import chess.core.model.Color;
import chess.core.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static chess.core.TestSupport.legalMoves;
import static chess.core.TestSupport.legalMovesFrom;
import static chess.core.TestSupport.position;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Detección de jaque y legalidad: el rey nunca puede quedar (ni quedarse) en jaque. */
class CheckDetectionTest {

    // ---- ¿hay jaque? ------------------------------------------------------------------------

    @Test
    void rookChecksAlongAnOpenFile() {
        assertTrue(position("4k3/8/8/8/8/8/8/4R1K1 b - - 0 1").isInCheck(Color.BLACK));
    }

    @Test
    void aBlockingPieceCancelsTheCheck() {
        assertFalse(position("4k3/8/8/4p3/8/8/8/4R1K1 b - - 0 1").isInCheck(Color.BLACK));
    }

    @Test
    void bishopAndQueenCheckOnDiagonals() {
        assertTrue(position("7k/8/8/8/8/8/8/B3K3 b - - 0 1").isInCheck(Color.BLACK));   // diagonal a1-h8 despejada
        assertFalse(position("4k3/8/8/8/8/8/8/B3K3 b - - 0 1").isInCheck(Color.BLACK)); // e8 no está en esa diagonal
        assertTrue(position("4k3/8/8/7Q/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK));  // dama h5 -> e8 por la diagonal
        assertFalse(position("4k3/5p2/8/7Q/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK)); // el peón f7 bloquea
    }

    @Test
    void knightChecksByJumping() {
        assertTrue(position("4k3/8/5N2/8/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK));
        // Aunque haya piezas en el medio, el caballo da jaque igual.
        assertTrue(position("4k3/3pp3/5N2/8/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK));
    }

    @Test
    void pawnsCheckDiagonallyForwardOnly() {
        assertTrue(position("4k3/3P4/8/8/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK));
        assertFalse(position("4k3/4P3/8/8/8/8/8/4K3 b - - 0 1").isInCheck(Color.BLACK));   // de frente no
        assertTrue(position("4k3/8/8/8/8/8/3p4/4K3 w - - 0 1").isInCheck(Color.WHITE));    // peón negro mira hacia abajo
        assertFalse(position("4k3/8/8/8/8/8/8/3pK3 w - - 0 1").isInCheck(Color.WHITE));    // peón negro al costado
    }

    @Test
    void startingPositionHasNoCheck() {
        Position start = position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertFalse(start.isInCheck(Color.WHITE));
        assertFalse(start.isInCheck(Color.BLACK));
    }

    @Test
    void eachSideIsCheckedIndependently() {
        Position p = position("4k3/8/8/8/8/8/8/4R1K1 b - - 0 1");
        assertTrue(p.isInCheck(Color.BLACK));
        assertFalse(p.isInCheck(Color.WHITE));
    }

    // ---- legalidad --------------------------------------------------------------------------

    @Test
    @DisplayName("Con el rey en jaque solo valen los movimientos que lo resuelven")
    void onlyMovesThatResolveCheckAreLegal() {
        // Dama negra en e8 da jaque; el peón d2 no puede ayudar. El rey solo puede ir a d1, f1 o f2.
        assertEquals(List.of("e1d1", "e1f1", "e1f2"), legalMoves("4q1k1/8/8/8/8/8/3P4/4K3 w - - 0 1"));
    }

    @Test
    void aCheckCanBeResolvedByBlockingOrCapturing() {
        // Torre negra en e8 da jaque a lo largo de la columna e: se puede bloquear con la dama (e2, e6) o mover el rey.
        List<String> moves = legalMoves("4r1k1/8/8/8/8/8/Q7/4K3 w - - 0 1");
        assertTrue(moves.contains("a2e2"));  // bloquea con la dama en e2
        assertTrue(moves.contains("a2e6"));  // o más lejos, en e6
        assertTrue(moves.contains("e1d1"));  // o el rey se aparta
        assertFalse(moves.contains("a2a8")); // no resuelve el jaque
    }

    @Test
    @DisplayName("Una pieza clavada solo puede moverse a lo largo de la línea de la clavada")
    void pinnedPieceMovesOnlyAlongThePin() {
        // Torre blanca e2 clavada por la torre negra e7 contra el rey e1.
        assertEquals(List.of("e2e3", "e2e4", "e2e5", "e2e6", "e2e7"),
                legalMovesFrom("4k3/4r3/8/8/8/8/4R3/4K3 w - - 0 1", "e2"));
    }

    @Test
    void aPinnedKnightCannotMoveAtAll() {
        assertEquals(List.of(), legalMovesFrom("4k3/4r3/8/8/8/8/4N3/4K3 w - - 0 1", "e2"));
    }

    @Test
    void kingCannotStepIntoCheck() {
        // Torre negra en a2 domina toda la fila 2.
        assertEquals(List.of("e1d1", "e1f1"), legalMovesFrom("4k3/8/8/8/8/8/r7/4K3 w - - 0 1", "e1"));
    }

    @Test
    void kingCannotCaptureAProtectedPiece() {
        // Peón negro en e2 protegido por una torre en e8: capturarlo dejaría al rey en jaque.
        assertFalse(legalMovesFrom("4r1k1/8/8/8/8/8/4p3/4K3 w - - 0 1", "e1").contains("e1e2"));
        // Si no está protegido, sí puede capturarlo.
        assertTrue(legalMovesFrom("6k1/8/8/8/8/8/4p3/4K3 w - - 0 1", "e1").contains("e1e2"));
    }

    @Test
    void kingsCannotStandNextToEachOther() {
        // Rey negro en e3: las casillas d2, e2 y f2 quedan prohibidas para el rey blanco de e1.
        assertEquals(List.of("e1d1", "e1f1"), legalMovesFrom("8/8/8/8/8/4k3/8/4K3 w - - 0 1", "e1"));
    }

    @Test
    void movingAPieceCannotExposeTheKingToADiscoveredCheck() {
        // El alfil blanco e2 tapa la columna e; si se mueve, la torre negra daría jaque.
        List<String> moves = legalMovesFrom("4r1k1/8/8/8/8/8/4B3/4K3 w - - 0 1", "e2");
        assertEquals(List.of(), moves);
    }
}
