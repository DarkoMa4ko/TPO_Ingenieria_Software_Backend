package chess.core.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitboardTest {

    @Test
    @DisplayName("Un bitboard vacío no tiene bits y sus extremos valen -1")
    void emptyBoard() {
        assertTrue(Bitboard.EMPTY.isEmpty());
        assertEquals(0, Bitboard.EMPTY.cardinality());
        assertEquals(-1, Bitboard.EMPTY.lowestBit());
        assertEquals(-1, Bitboard.EMPTY.highestBit());
    }

    @Test
    void ofBitSetsExactlyOneBit() {
        Bitboard board = Bitboard.ofBit(26);
        assertTrue(board.has(26));
        assertFalse(board.has(25));
        assertFalse(board.has(27));
        assertEquals(1, board.cardinality());
        assertEquals(26, board.lowestBit());
        assertEquals(26, board.highestBit());
    }

    @Test
    @DisplayName("Funciona más allá de 64 bits (tableros grandes)")
    void supportsMoreThan64Bits() {
        Bitboard board = Bitboard.ofBit(5).with(79);
        assertTrue(board.has(79));
        assertEquals(79, board.highestBit());
        assertEquals(5, board.lowestBit());
        assertEquals(2, board.cardinality());
    }

    @Test
    void setAlgebra() {
        Bitboard a = Bitboard.ofBits(1, 2, 3);
        Bitboard b = Bitboard.ofBits(3, 4);
        assertEquals(Bitboard.ofBits(1, 2, 3, 4), a.or(b));
        assertEquals(Bitboard.ofBit(3), a.and(b));
        assertEquals(Bitboard.ofBits(1, 2, 4), a.xor(b));
        assertEquals(Bitboard.ofBits(1, 2), a.andNot(b));
        assertTrue(a.intersects(b));
        assertFalse(a.intersects(Bitboard.ofBit(9)));
    }

    @Test
    void withAndWithout() {
        Bitboard board = Bitboard.EMPTY.with(3).with(7);
        assertEquals(Bitboard.ofBits(3, 7), board);
        assertEquals(Bitboard.ofBit(7), board.without(3));
        assertEquals(board, board.without(40)); // quitar un bit ausente no cambia nada
    }

    @Test
    void shifts() {
        Bitboard board = Bitboard.ofBits(0, 1);
        assertEquals(Bitboard.ofBits(8, 9), board.shiftLeft(8));
        assertEquals(Bitboard.ofBit(0), Bitboard.ofBits(8, 9).shiftRight(9)); // el bit 8 sale y se pierde
        assertEquals(Bitboard.ofBits(0, 1), Bitboard.ofBits(8, 9).shiftRight(8));
        assertEquals(Bitboard.EMPTY, Bitboard.ofBit(3).shiftRight(4)); // lo que sale por abajo se pierde
        assertEquals(board, board.shiftLeft(0));
    }

    @Test
    void filledSetsTheFirstNBits() {
        Bitboard board = Bitboard.filled(64);
        assertEquals(64, board.cardinality());
        assertTrue(board.has(0));
        assertTrue(board.has(63));
        assertFalse(board.has(64));
        assertEquals(Bitboard.EMPTY, Bitboard.filled(0));
    }

    @Test
    void forEachBitVisitsInAscendingOrder() {
        List<Integer> visited = new ArrayList<>();
        Bitboard.ofBits(9, 2, 70, 5).forEachBit(visited::add);
        assertEquals(List.of(2, 5, 9, 70), visited);
        assertEquals(4, Bitboard.ofBits(9, 2, 70, 5).toIndices().length);
        assertEquals(2, Bitboard.ofBits(9, 2, 70, 5).toIndices()[0]);
    }

    @Test
    void equalityIsByValue() {
        assertEquals(Bitboard.ofBits(1, 2), Bitboard.ofBit(1).or(Bitboard.ofBit(2)));
        assertEquals(Bitboard.ofBits(1, 2).hashCode(), Bitboard.ofBit(2).or(Bitboard.ofBit(1)).hashCode());
        assertFalse(Bitboard.ofBit(1).equals(Bitboard.ofBit(2)));
    }

    @Test
    void negativeIndicesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Bitboard.ofBit(-1));
        assertThrows(IllegalArgumentException.class, () -> Bitboard.EMPTY.with(-3));
        assertFalse(Bitboard.ofBit(1).has(-1));
    }

    @Test
    void gridDrawsTopRankFirst() {
        Bitboard board = Bitboard.ofBit(0).with(3); // a1 y d1 en un tablero de 4x2
        assertEquals(". . . .\n1 . . 1", board.toGrid(4, 2));
    }
}
