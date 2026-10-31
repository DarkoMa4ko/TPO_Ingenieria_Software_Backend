package chess.core.standard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardPositionValidatorTest {

    @Test
    void acceptsInitialPosition() {
        assertDoesNotThrow(
                () -> StandardChess.newGame(
                        StandardChess.START_FEN
                )
        );
    }

    @Test
    void rejectsPositionWithoutWhiteKing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "4k3/8/8/8/8/8/8/8 w - - 0 1"
                )
        );
    }

    @Test
    void rejectsPositionWithoutBlackKing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "8/8/8/8/8/8/8/4K3 w - - 0 1"
                )
        );
    }

    @Test
    void rejectsPositionWithTwoWhiteKings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "4k3/8/8/8/8/8/4K3/4K3 w - - 0 1"
                )
        );
    }

    @Test
    void rejectsPositionWithTwoBlackKings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "4k3/4k3/8/8/8/8/8/4K3 w - - 0 1"
                )
        );
    }

    @Test
    void rejectsWhenBothKingsAreInCheck() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "4k3/4R3/8/8/8/8/4r3/4K3 w - - 0 1"
                )
        );
    }

    @Test
    void rejectsWhenPreviousPlayerLeftOwnKingInCheck() {
        /*
         * Les toca a blancas.
         *
         * Por lo tanto el movimiento anterior fue de negras,
         * pero el rey negro ya está atacado por la torre blanca.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> StandardChess.newGame(
                        "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1"
                )
        );
    }
}