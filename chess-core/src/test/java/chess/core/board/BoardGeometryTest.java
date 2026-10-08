package chess.core.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardGeometryTest {

    private final BoardGeometry g = BoardGeometry.STANDARD;

    private Square sq(String text) {
        return Square.parse(text);
    }

    private Bitboard bits(BoardGeometry geometry, String... squares) {
        Bitboard result = Bitboard.EMPTY;
        for (String s : squares) {
            result = result.or(geometry.bit(Square.parse(s)));
        }
        return result;
    }

    @Test
    void indexingIsRowByRow() {
        assertEquals(0, g.index(sq("a1")));
        assertEquals(7, g.index(sq("h1")));
        assertEquals(8, g.index(sq("a2")));
        assertEquals(63, g.index(sq("h8")));
        assertEquals(sq("h8"), g.square(63));
        assertEquals(64, g.squareCount());
    }

    @Test
    void squareAndIndexAreInverse() {
        for (int i = 0; i < g.squareCount(); i++) {
            assertEquals(i, g.index(g.square(i)));
        }
    }

    @Test
    void rejectsSquaresOutsideTheBoard() {
        assertFalse(g.contains(new Square(8, 0)));
        assertFalse(g.contains(new Square(0, 8)));
        assertThrows(IllegalArgumentException.class, () -> g.index(new Square(8, 0)));
        assertThrows(IllegalArgumentException.class, () -> g.square(64));
        assertThrows(IllegalArgumentException.class, () -> g.square(-1));
    }

    @Test
    void sameShapeReusesTheSameInstance() {
        assertSame(BoardGeometry.of(8, 8), BoardGeometry.STANDARD);
        assertEquals(BoardGeometry.of(10, 8), BoardGeometry.of(10, 8));
        assertThrows(IllegalArgumentException.class, () -> BoardGeometry.of(0, 8));
        assertThrows(IllegalArgumentException.class, () -> BoardGeometry.of(27, 8));
    }

    @Test
    void fileAndRankMasks() {
        assertEquals(8, g.fileMask(0).cardinality());
        assertEquals(8, g.rankMask(7).cardinality());
        assertEquals(bits(g, "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8"), g.fileMask(0));
        assertEquals(bits(g, "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"), g.rankMask(0));
    }

    @Test
    void complementIsRelativeToTheBoard() {
        assertEquals(63, g.complement(bits(g, "e4")).cardinality());
        assertEquals(Bitboard.EMPTY, g.complement(g.all()));
    }

    @Test
    @DisplayName("shift: un paso al este desde la columna h no reaparece en la columna a (sin wrap-around)")
    void shiftDoesNotWrapAroundTheSides() {
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("h4")), Direction.EAST));
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("a4")), Direction.WEST));
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("h4")), Direction.NORTH_EAST));
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("a4")), Direction.SOUTH_WEST));
    }

    @Test
    void shiftDropsSquaresThatLeaveThroughTopOrBottom() {
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("e8")), Direction.NORTH));
        assertEquals(Bitboard.EMPTY, g.shift(g.bit(sq("e1")), Direction.SOUTH));
        assertEquals(g.bit(sq("e5")), g.shift(g.bit(sq("e4")), Direction.NORTH));
        assertEquals(g.bit(sq("d3")), g.shift(g.bit(sq("e4")), Direction.SOUTH_WEST));
    }

    @Test
    void shiftMovesWholeSetsAtOnce() {
        Bitboard row = g.rankMask(1); // los 8 peones blancos iniciales
        assertEquals(g.rankMask(2), g.shift(row, Direction.NORTH));
        assertEquals(7, g.shift(row, Direction.EAST).cardinality());
    }

    @Test
    void knightJumpsFromACornerStayInsideTheBoard() {
        Bitboard jumps = Bitboard.EMPTY;
        for (Direction d : Direction.KNIGHT_JUMPS) {
            jumps = jumps.or(g.shift(g.bit(sq("a1")), d));
        }
        assertEquals(bits(g, "b3", "c2"), jumps);
    }

    @Test
    void raysGoToTheEdge() {
        assertEquals(bits(g, "d5", "d6", "d7", "d8"), g.ray(g.index(sq("d4")), Direction.NORTH));
        assertEquals(bits(g, "b2", "c3", "d4", "e5", "f6", "g7", "h8"),
                g.ray(g.index(sq("a1")), Direction.NORTH_EAST));
        assertEquals(Bitboard.EMPTY, g.ray(g.index(sq("h4")), Direction.EAST));
        assertTrue(g.isIncreasing(Direction.NORTH));
        assertTrue(g.isIncreasing(Direction.EAST));
        assertFalse(g.isIncreasing(Direction.SOUTH));
        assertFalse(g.isIncreasing(Direction.WEST));
        assertFalse(g.isIncreasing(Direction.SOUTH_EAST));
    }

    @Test
    void betweenReturnsOnlyTheSquaresInTheMiddle() {
        assertEquals(bits(g, "b1", "c1", "d1", "e1", "f1", "g1"), g.between(sq("a1"), sq("h1")));
        assertEquals(bits(g, "g1", "f1"), g.between(sq("h1"), sq("e1")));
        assertEquals(bits(g, "b2", "c3"), g.between(sq("a1"), sq("d4")));
        assertEquals(Bitboard.EMPTY, g.between(sq("a1"), sq("b1")));  // contiguas
        assertEquals(Bitboard.EMPTY, g.between(sq("a1"), sq("c2")));  // no alineadas
        assertEquals(Bitboard.EMPTY, g.between(sq("e1"), sq("e1")));  // misma casilla
    }

    @Test
    @DisplayName("Un tablero de 10x8 indexa y desplaza con su propio ancho")
    void nonStandardBoardUsesItsOwnWidth() {
        BoardGeometry wide = BoardGeometry.of(10, 8);
        assertEquals(80, wide.squareCount());
        assertEquals(9, wide.index(sq("j1")));
        assertEquals(10, wide.index(sq("a2")));
        assertEquals(79, wide.index(sq("j8")));
        assertTrue(wide.contains(sq("j8")));
        assertFalse(wide.contains(sq("k1")));
        // En 10x8 la columna j existe: un paso al este desde i4 llega a j4, y desde j4 desaparece.
        assertEquals(wide.bit(sq("j4")), wide.shift(wide.bit(sq("i4")), Direction.EAST));
        assertEquals(Bitboard.EMPTY, wide.shift(wide.bit(sq("j4")), Direction.EAST));
        assertEquals(wide.bit(sq("a5")), wide.shift(wide.bit(sq("a4")), Direction.NORTH));
    }

    @Test
    void squaresListsInAscendingOrder() {
        assertEquals(java.util.List.of(sq("a1"), sq("c3"), sq("h8")), g.squares(bits(g, "h8", "a1", "c3")));
    }
}
