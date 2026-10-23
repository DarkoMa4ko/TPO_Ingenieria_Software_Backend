package chess.core.game;

import chess.core.model.Position;
import chess.core.standard.StandardChess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la condición de tablas por triple repetición.
 *
 * <p>La condición se prueba de forma aislada construyendo directamente
 * un GameContext con diferentes historiales de posiciones. No necesita
 * Game completo, base de datos, API ni ninguna otra infraestructura.
 */
class ThreefoldRepetitionTest {

    @Test
    void oneOccurrenceDoesNotEndTheGame() {
        // Arrange
        Position position = StandardChess.initialPosition();

        GameContext context = new GameContext(
                position,
                List.of(),
                List.of(position)
        );

        ThreefoldRepetition condition =
                new ThreefoldRepetition();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void twoOccurrencesDoNotEndTheGame() {
        // Arrange
        Position position = StandardChess.initialPosition();

        GameContext context = new GameContext(
                position,
                List.of(),
                List.of(
                        position,
                        position
                )
        );

        ThreefoldRepetition condition =
                new ThreefoldRepetition();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void threeOccurrencesEndTheGameInADraw() {
        // Arrange
        Position position = StandardChess.initialPosition();

        GameContext context = new GameContext(
                position,
                List.of(),
                List.of(
                        position,
                        position,
                        position
                )
        );

        ThreefoldRepetition condition =
                new ThreefoldRepetition();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isPresent());

        assertEquals(
                Outcome.DRAW,
                result.orElseThrow().outcome()
        );

        assertEquals(
                "Triple repetición",
                result.orElseThrow().reason()
        );
    }

    @Test
    void differentPositionsDoNotCountAsRepetitions() {
        // Arrange
        Position current =
                StandardChess.initialPosition();

        Position different = StandardChess.fen().parse(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1"
        );

        GameContext context = new GameContext(
                current,
                List.of(),
                List.of(
                        current,
                        different,
                        different
                )
        );

        ThreefoldRepetition condition =
                new ThreefoldRepetition();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isEmpty());
    }
}