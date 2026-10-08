package chess.core.board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SquareTest {

    @Test
    void parsesAlgebraicNotation() {
        assertEquals(new Square(0, 0), Square.parse("a1"));
        assertEquals(new Square(4, 3), Square.parse("e4"));
        assertEquals(new Square(7, 7), Square.parse("H8"));
        assertEquals(new Square(9, 9), Square.parse("j10"));
    }

    @Test
    void toStringIsTheInverseOfParse() {
        for (String text : new String[] {"a1", "h8", "e4", "c7", "j10"}) {
            assertEquals(text, Square.parse(text).toString());
        }
    }

    @Test
    void rejectsMalformedText() {
        for (String bad : new String[] {"", "e", "44", "e0", "e-1", "ee", "é4", "e4x"}) {
            assertThrows(IllegalArgumentException.class, () -> Square.parse(bad), "debería rechazar '" + bad + "'");
        }
        assertThrows(IllegalArgumentException.class, () -> Square.parse(null));
    }

    @Test
    void rejectsNegativeCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new Square(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Square(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Square(26, 0));
    }
}
