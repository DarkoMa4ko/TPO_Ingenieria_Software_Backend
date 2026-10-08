package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.move.StandardMove;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static chess.core.TestSupport.position;
import static chess.core.TestSupport.sq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionTest {

    private static final Piece WHITE_ROOK = new Piece(StandardPieces.ROOK, Color.WHITE);
    private static final Piece BLACK_PAWN = new Piece(StandardPieces.PAWN, Color.BLACK);

    @Test
    void initialPositionHasThirtyTwoPiecesInTheRightPlaces() {
        Position start = StandardChess.initialPosition();
        assertEquals(32, start.occupied().cardinality());
        assertEquals(16, start.occupiedBy(Color.WHITE).cardinality());
        assertEquals(16, start.occupiedBy(Color.BLACK).cardinality());
        assertEquals(8, start.pieces(Color.WHITE, StandardPieces.PAWN).cardinality());
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.WHITE)), start.pieceAt(sq("e1")));
        assertEquals(Optional.of(new Piece(StandardPieces.QUEEN, Color.BLACK)), start.pieceAt(sq("d8")));
        assertEquals(Optional.empty(), start.pieceAt(sq("e4")));
        assertEquals(Color.WHITE, start.sideToMove());
    }

    @Test
    void builderPlacesRemovesAndRelocates() {
        Position.Builder b = Position.builder(BoardGeometry.STANDARD);
        b.place(sq("a1"), WHITE_ROOK);
        b.place(sq("a5"), BLACK_PAWN);
        assertEquals(Optional.of(WHITE_ROOK), b.pieceAt(sq("a1")));

        b.relocate(sq("a1"), sq("a5")); // captura el peón
        Position p = b.build();
        assertEquals(Optional.empty(), p.pieceAt(sq("a1")));
        assertEquals(Optional.of(WHITE_ROOK), p.pieceAt(sq("a5")));
        assertEquals(1, p.occupied().cardinality());
        assertEquals(0, p.pieces(Color.BLACK, StandardPieces.PAWN).cardinality());

        Position.Builder again = p.toBuilder();
        again.remove(sq("a5"));
        assertEquals(0, again.build().occupied().cardinality());
        assertThrows(IllegalStateException.class, () -> Position.builder(BoardGeometry.STANDARD).relocate(sq("a1"), sq("a2")));
    }

    @Test
    void playingAMoveDoesNotModifyTheOriginalPosition() {
        Position start = StandardChess.initialPosition();
        Position after = start.play(new StandardMove(sq("e2"), sq("e4")));

        assertEquals(Optional.of(new Piece(StandardPieces.PAWN, Color.WHITE)), start.pieceAt(sq("e2")));
        assertEquals(Color.WHITE, start.sideToMove());
        assertEquals(Optional.empty(), after.pieceAt(sq("e2")));
        assertEquals(Optional.of(new Piece(StandardPieces.PAWN, Color.WHITE)), after.pieceAt(sq("e4")));
        assertEquals(Color.BLACK, after.sideToMove());
    }

    @Test
    void unmovedBitsAreClearedWhenAPieceMovesOrIsCaptured() {
        Position start = StandardChess.initialPosition();
        assertTrue(start.unmoved().has(start.geometry().index(sq("h1"))));
        Position moved = start.play(new StandardMove(sq("h1"), sq("h3")));
        assertFalse(moved.unmoved().has(moved.geometry().index(sq("h1"))));
        assertFalse(moved.unmoved().has(moved.geometry().index(sq("h3"))));
        assertTrue(moved.unmoved().has(moved.geometry().index(sq("a1")))); // la otra torre sigue sin moverse
    }

    @Test
    void equalPositionsAreEqual() {
        Position a = StandardChess.initialPosition();
        Position b = position(StandardChess.START_FEN);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, a.play(new StandardMove(sq("e2"), sq("e4"))));
    }

    @Test
    void attackMapIncludesSquaresWithPiecesOfTheSameColor() {
        // La torre "protege" a su propio peón: esa casilla figura como atacada.
        Position p = position("4k3/8/8/8/8/8/P7/R3K3 w - - 0 1");
        assertTrue(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("a2"))));
        assertTrue(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("d1"))));
        assertFalse(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("a3")))); // detrás de su propio peón
    }

    @Test
    void sidesWithoutARoyalPieceAreNeverInCheck() {
        Position p = position("8/8/8/8/8/8/8/R7 w - - 0 1");
        assertFalse(p.isInCheck(Color.WHITE));
        assertFalse(p.isInCheck(Color.BLACK));
    }

    @Test
    void pieceTypesListsOnlyWhatIsOnTheBoard() {
        Position p = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        assertEquals(2, p.pieceTypes().size());
        assertTrue(p.pieceTypes().contains(StandardPieces.ROOK));
        assertTrue(p.pieceTypes().contains(StandardPieces.KING));
        assertEquals(Bitboard.EMPTY, p.pieces(StandardPieces.QUEEN));
    }
}
