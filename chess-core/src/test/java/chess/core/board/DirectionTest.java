package chess.core.board;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionTest {

    @Test
    void standardGroupsHaveTheExpectedSize() {
        assertEquals(4, Direction.ORTHOGONAL.size());
        assertEquals(4, Direction.DIAGONAL.size());
        assertEquals(8, Direction.ALL.size());
        assertEquals(8, new HashSet<>(Direction.ALL).size());
    }

    @Test
    void knightJumpsAreTheEightLShapes() {
        assertEquals(8, Direction.KNIGHT_JUMPS.size());
        assertTrue(Direction.KNIGHT_JUMPS.contains(new Direction(1, 2)));
        assertTrue(Direction.KNIGHT_JUMPS.contains(new Direction(-2, 1)));
        assertTrue(Direction.KNIGHT_JUMPS.contains(new Direction(2, -1)));
    }

    @Test
    void symmetriesGeneratesAllVariantsWithoutRepeats() {
        assertEquals(8, Direction.symmetries(1, 3).size()); // el "camello"
        assertEquals(4, Direction.symmetries(1, 1).size()); // diagonales
        assertEquals(4, Direction.symmetries(1, 0).size()); // ortogonales
    }

    @Test
    void oppositeInvertsBothSteps() {
        assertEquals(Direction.SOUTH_WEST, Direction.NORTH_EAST.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
    }

    @Test
    void nullDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Direction(0, 0));
    }
}
