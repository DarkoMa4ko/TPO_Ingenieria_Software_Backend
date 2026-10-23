package chess.core.game;

import chess.core.model.Position;
import chess.core.standard.StandardChess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la regla de los cincuenta movimientos.
 *
 * <p>La condición se prueba de forma aislada mediante un GameContext.
 * El contador utilizado por la regla está contenido en Position y
 * representa la cantidad de medios movimientos transcurridos desde
 * la última captura o movimiento de peón.
 */
class FiftyMoveRuleTest {

    @Test
    void zeroHalfMovesDoNotEndTheGame() {
        // Arrange
        Position position = positionWithHalfmoveClock(0);

        GameContext context = contextFor(position);

        FiftyMoveRule condition =
                new FiftyMoveRule();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ninetyNineHalfMovesDoNotEndTheGame() {
        // Arrange
        Position position = positionWithHalfmoveClock(99);

        GameContext context = contextFor(position);

        FiftyMoveRule condition =
                new FiftyMoveRule();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void oneHundredHalfMovesEndTheGameInADraw() {
        // Arrange
        Position position = positionWithHalfmoveClock(100);

        GameContext context = contextFor(position);

        FiftyMoveRule condition =
                new FiftyMoveRule();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isPresent());

        GameResult gameResult =
                result.orElseThrow();

        assertEquals(
                Outcome.DRAW,
                gameResult.outcome()
        );

        assertEquals(
                "Regla de los 50 movimientos",
                gameResult.reason()
        );
    }

    @Test
    void moreThanOneHundredHalfMovesAlsoEndTheGameInADraw() {
        // Arrange
        Position position = positionWithHalfmoveClock(101);

        GameContext context = contextFor(position);

        FiftyMoveRule condition =
                new FiftyMoveRule();

        // Act
        Optional<GameResult> result =
                condition.evaluate(context);

        // Assert
        assertTrue(result.isPresent());

        assertEquals(
                Outcome.DRAW,
                result.orElseThrow().outcome()
        );
    }

    /**
     * Construye una posición sencilla con el contador indicado.
     *
     * <p>No necesitamos generar cien movimientos reales para probar
     * FiftyMoveRule: esta prueba verifica únicamente la responsabilidad
     * de la condición de fin.
     */
    private Position positionWithHalfmoveClock(
            int halfmoveClock
    ) {
        return StandardChess.fen().parse(
                "4k3/8/8/8/8/8/8/4K3 w - - "
                        + halfmoveClock
                        + " 1"
        );
    }

    /**
     * Construye el contexto mínimo necesario para evaluar la condición.
     *
     * <p>Los movimientos legales y el historial no participan en esta
     * regla; se incluyen porque forman parte del contrato de GameContext.
     */
    private GameContext contextFor(
            Position position
    ) {
        return new GameContext(
                position,
                List.of(),
                List.of(position)
        );
    }
}