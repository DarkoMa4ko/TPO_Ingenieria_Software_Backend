package chess.core.movement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static chess.core.TestSupport.targets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada pieza se mueve como corresponde. Las posiciones se arman con FEN (sin reyes cuando no hacen falta). */
class PieceMovementTest {

    // ---- torre ---------------------------------------------------------------------------

    @Test
    void rookSlidesAlongRanksAndFilesUntilTheEdge() {
        List<String> t = targets("8/8/8/8/3R4/8/8/8 w - - 0 1", "d4");
        assertEquals(14, t.size());
        assertEquals(List.of("a4", "b4", "c4", "d1", "d2", "d3", "d5", "d6", "d7", "d8", "e4", "f4", "g4", "h4"), t);
    }

    @Test
    @DisplayName("La torre se detiene ante una pieza propia y captura la rival (sin pasar de ella)")
    void rookStopsAtBlockers() {
        // Peón negro d5, torre blanca d4, peón negro f4, peón blanco d1.
        List<String> t = targets("8/8/8/3p4/3R1p2/8/8/3P4 w - - 0 1", "d4");
        assertEquals(List.of("a4", "b4", "c4", "d2", "d3", "d5", "e4", "f4"), t);
    }

    @Test
    void rookNeverMovesDiagonally() {
        List<String> t = targets("8/8/8/8/3R4/8/8/8 w - - 0 1", "d4");
        assertTrue(!t.contains("e5") && !t.contains("c3"));
    }

    // ---- alfil ---------------------------------------------------------------------------

    @Test
    void bishopSlidesAlongDiagonals() {
        List<String> t = targets("8/8/8/8/3B4/8/8/8 w - - 0 1", "d4");
        assertEquals(13, t.size());
        assertEquals(List.of("a1", "a7", "b2", "b6", "c3", "c5", "e3", "e5", "f2", "f6", "g1", "g7", "h8"), t);
    }

    @Test
    void bishopIsBlockedByOwnPiecesAndCapturesEnemies() {
        // Alfil d4, peón propio en f6, peón rival en b2.
        List<String> t = targets("8/8/5P2/8/3B4/8/1p6/8 w - - 0 1", "d4");
        assertEquals(List.of("a7", "b2", "b6", "c3", "c5", "e3", "e5", "f2", "g1"), t);
    }

    @Test
    void bishopStaysOnItsColor() {
        for (String square : targets("8/8/8/8/3B4/8/8/8 w - - 0 1", "d4")) {
            int file = square.charAt(0) - 'a';
            int rank = square.charAt(1) - '1';
            assertEquals((3 + 3) % 2, (file + rank) % 2); // d4 = (3, 3)
        }
    }

    // ---- reina ---------------------------------------------------------------------------

    @Test
    void queenCombinesRookAndBishop() {
        assertEquals(27, targets("8/8/8/8/3Q4/8/8/8 w - - 0 1", "d4").size());
        assertEquals(21, targets("8/8/8/8/Q7/8/8/8 w - - 0 1", "a4").size());
    }

    // ---- caballo -------------------------------------------------------------------------

    @Test
    void knightJumpsInL() {
        assertEquals(List.of("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5"),
                targets("8/8/8/8/3N4/8/8/8 w - - 0 1", "d4"));
    }

    @Test
    void knightJumpsOverPieces() {
        // Rodeado de peones propios y rivales: igual salta.
        List<String> t = targets("8/8/2ppp3/2pNp3/2ppp3/8/8/8 w - - 0 1", "d5");
        assertEquals(8, t.size());
    }

    @Test
    @DisplayName("El caballo no 'da la vuelta' al tablero en los bordes (sin wrap-around)")
    void knightDoesNotWrapAroundTheEdges() {
        assertEquals(List.of("b3", "c2"), targets("8/8/8/8/8/8/8/N7 w - - 0 1", "a1"));
        assertEquals(List.of("f2", "g3"), targets("8/8/8/8/8/8/8/7N w - - 0 1", "h1"));
        assertEquals(List.of("b2", "b6", "c3", "c5"), targets("8/8/8/8/N7/8/8/8 w - - 0 1", "a4"));
        assertEquals(List.of("f3", "f5", "g2", "g6"), targets("8/8/8/8/7N/8/8/8 w - - 0 1", "h4"));
        assertEquals(List.of("f7", "g6"), targets("7N/8/8/8/8/8/8/8 w - - 0 1", "h8"));
    }

    @Test
    void knightCannotLandOnOwnPieces() {
        // Posición inicial: el caballo de b1 no puede ir a d2 (peón propio).
        List<String> t = targets("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "b1");
        assertEquals(List.of("a3", "c3"), t);
    }

    // ---- rey -----------------------------------------------------------------------------

    @Test
    void kingMovesOneSquareInAnyDirection() {
        assertEquals(List.of("c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5"),
                targets("4k3/8/8/8/3K4/8/8/8 w - - 0 1", "d4"));
    }

    @Test
    void kingInTheCornerHasThreeMoves() {
        assertEquals(List.of("a2", "b1", "b2"), targets("7k/8/8/8/8/8/8/K7 w - - 0 1", "a1"));
    }
}
