package chess.core.model;

import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieceTest {

    @Test
    void whitePiecesUseUppercaseAndBlackLowercase() {
        assertEquals('N', new Piece(StandardPieces.KNIGHT, Color.WHITE).symbol());
        assertEquals('n', new Piece(StandardPieces.KNIGHT, Color.BLACK).symbol());
    }

    @Test
    void onlyTheKingIsRoyal() {
        assertTrue(StandardPieces.KING.royal());
        for (PieceType type : StandardPieces.ALL) {
            if (type != StandardPieces.KING) {
                assertFalse(type.royal());
            }
        }
    }

    @Test
    void pieceTypeValidatesItsData() {
        MovementRule rule = StandardPieces.ROOK.movement();
        assertThrows(IllegalArgumentException.class, () -> new PieceType("X", 'x', rule));
        assertThrows(NullPointerException.class, () -> new PieceType(null, 'X', rule));
        assertThrows(NullPointerException.class, () -> new PieceType("X", 'X', null));
    }

    @Test
    void colorHelpers() {
        assertEquals(Color.BLACK, Color.WHITE.opposite());
        assertEquals(Color.WHITE, Color.BLACK.opposite());
        assertEquals(1, Color.WHITE.forward());
        assertEquals(-1, Color.BLACK.forward());
    }
}
