package chess.core.game;

import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Piece;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static chess.core.TestSupport.notations;
import static chess.core.TestSupport.play;
import static chess.core.TestSupport.playAll;
import static chess.core.TestSupport.sq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    // ---- turnos ----------------------------------------------------------------------------

    @Test
    void whiteMovesFirstAndTurnsAlternate() {
        Game game = StandardChess.newGame();

        assertEquals(Color.WHITE, game.turn());

        play(game, "e2e4");
        assertEquals(Color.BLACK, game.turn());

        play(game, "e7e5");
        assertEquals(Color.WHITE, game.turn());
    }

    @Test
    void aPlayerCannotMoveTwiceInARow() {
        Game game = StandardChess.newGame();

        play(game, "e2e4");

        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> play(game, "d2d4")
        );

        assertTrue(e.getMessage().contains("turno"));

        // El intento fallido no modifica el turno.
        assertEquals(Color.BLACK, game.turn());
    }

    @Test
    void openingPositionHasTwentyLegalMoves() {
        assertEquals(
                20,
                StandardChess.newGame().legalMoves().size()
        );
    }

    // ---- validación de movimientos inválidos -----------------------------------------------

    @Test
    void movingFromAnEmptySquareIsRejected() {
        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> play(
                        StandardChess.newGame(),
                        "e4e5"
                )
        );

        assertTrue(
                e.getMessage().contains("No hay ninguna pieza")
        );
    }

    @Test
    void aPieceCannotMoveAgainstItsOwnRules() {
        Game game = StandardChess.newGame();

        IllegalMoveException knight = assertThrows(
                IllegalMoveException.class,
                () -> play(game, "b1b3")
        );

        assertTrue(
                knight.getMessage().contains("Caballo")
        );

        // El peón no puede avanzar tres casillas.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "e2e5")
        );

        // La torre no puede atravesar su propio peón.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "a1a3")
        );

        // El alfil no puede atravesar su propio peón.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "c1e3")
        );

        // La reina tampoco puede atravesar piezas.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "d1d3")
        );

        assertEquals(
                20,
                game.legalMoves().size()
        );

        assertEquals(
                0,
                game.history().size()
        );
    }

    @Test
    void capturingYourOwnPieceIsRejected() {
        assertThrows(
                IllegalMoveException.class,
                () -> play(
                        StandardChess.newGame(),
                        "a1a2"
                )
        );

        assertThrows(
                IllegalMoveException.class,
                () -> play(
                        StandardChess.newGame(),
                        "e1e2"
                )
        );
    }

    @Test
    void squaresOutsideTheBoardAreRejected() {
        IllegalMoveException destination = assertThrows(
                IllegalMoveException.class,
                () -> play(
                        StandardChess.newGame(),
                        "e2e9"
                )
        );

        assertTrue(
                destination.getMessage().contains("e9")
        );

        assertThrows(
                IllegalMoveException.class,
                () -> play(
                        StandardChess.newGame(),
                        "i2i3"
                )
        );
    }

    @Test
    void aMoveThatLeavesYourKingInCheckIsRejectedWithAClearReason() {
        Game game = StandardChess.newGame(
                "4k3/4r3/8/8/8/8/4R3/4K3 w - - 0 1"
        );

        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> play(game, "e2d2")
        );

        assertTrue(
                e.getMessage().contains("jaque")
        );
    }

    @Test
    void theKingCannotMoveIntoCheck() {
        Game game = StandardChess.newGame(
                "4k3/8/8/8/8/8/r7/4K3 w - - 0 1"
        );

        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> play(game, "e1e2")
        );

        assertTrue(
                e.getMessage().contains("jaque")
        );
    }

    @Test
    void playingAMoveObjectThatIsNotLegalIsRejected() {
        Game game = StandardChess.newGame();

        Move illegal = new chess.core.move.StandardMove(
                sq("e2"),
                sq("e5")
        );

        assertThrows(
                IllegalMoveException.class,
                () -> game.play(illegal)
        );

        game.play(game.legalMoves().get(0));

        assertEquals(
                1,
                game.history().size()
        );
    }

    // ---- captura ---------------------------------------------------------------------------

    @Test
    void capturingRemovesTheEnemyPiece() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "e2e4 d7d5"
        );

        assertEquals(
                8,
                game.position()
                        .pieces(
                                Color.BLACK,
                                StandardPieces.PAWN
                        )
                        .cardinality()
        );

        play(game, "e4d5");

        assertEquals(
                7,
                game.position()
                        .pieces(
                                Color.BLACK,
                                StandardPieces.PAWN
                        )
                        .cardinality()
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                ),
                game.position().pieceAt(
                        sq("d5")
                )
        );

        assertEquals(
                15,
                game.position()
                        .occupiedBy(Color.BLACK)
                        .cardinality()
        );
    }

    // ---- jaque y fin de partida ------------------------------------------------------------

    @Test
    void checkIsDetectedAndOnlyAnswersToItAreLegal() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "e2e4 f7f6 d1h5"
        );

        assertTrue(game.isInCheck());
        assertFalse(game.isOver());

        assertEquals(
                List.of("g7g6"),
                notations(game.legalMoves())
        );
    }

    @Test
    @DisplayName("Mate del loco: las negras ganan en dos jugadas")
    void foolsMate() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "f2f3 e7e5 g2g4 d8h4"
        );

        assertTrue(game.isOver());
        assertTrue(game.isInCheck());

        assertEquals(
                Outcome.BLACK_WINS,
                game.result()
                        .orElseThrow()
                        .outcome()
        );

        assertEquals(
                "Jaque mate",
                game.result()
                        .orElseThrow()
                        .reason()
        );

        assertEquals(
                List.of(),
                game.legalMoves()
        );
    }

    @Test
    @DisplayName("Mate del pastor: las blancas ganan")
    void scholarsMate() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "e2e4 e7e5 d1h5 b8c6 f1c4 g8f6 h5f7"
        );

        assertEquals(
                Outcome.WHITE_WINS,
                game.result()
                        .orElseThrow()
                        .outcome()
        );

        assertEquals(
                7,
                game.history().size()
        );
    }

    @Test
    void noMovesAreAllowedAfterTheGameEnds() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "f2f3 e7e5 g2g4 d8h4"
        );

        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> play(game, "a2a3")
        );

        assertTrue(
                e.getMessage().contains("terminó")
        );
    }

    @Test
    void stalemateIsADraw() {
        Game game = StandardChess.newGame(
                "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
        );

        assertTrue(game.isOver());
        assertFalse(game.isInCheck());

        assertEquals(
                Outcome.DRAW,
                game.result()
                        .orElseThrow()
                        .outcome()
        );
    }

    @Test
    void stalemateReachedByAMove() {
        Game game = StandardChess.newGame(
                "7k/8/5K2/6Q1/8/8/8/8 w - - 0 1"
        );

        assertFalse(game.isOver());

        // La dama en g6 y el rey en f6 dejan al rey negro
        // sin casillas legales y sin jaque.
        play(game, "g5g6");

        assertTrue(game.isOver());

        assertEquals(
                Outcome.DRAW,
                game.result()
                        .orElseThrow()
                        .outcome()
        );
    }

    @Test
    @DisplayName("Triple repetición: repetir dos veces el ciclo de caballos termina en tablas")
    void gameEndsInDrawAfterThreefoldRepetition() {
        // Arrange
        Game game = StandardChess.newGame();

        // Act - primer ciclo.
        // Los caballos salen de sus casillas iniciales y luego regresan.
        play(game, "g1f3");
        play(game, "g8f6");
        play(game, "f3g1");
        play(game, "f6g8");

        /*
         * La posición inicial apareció hasta este punto dos veces:
         *
         * 1. Al comenzar la partida.
         * 2. Al terminar el primer ciclo de cuatro movimientos.
         *
         * Dos apariciones todavía no deben finalizar la partida.
         */
        assertFalse(game.isOver());

        // Segundo ciclo.
        // Al completarlo, la posición inicial vuelve a aparecer.
        play(game, "g1f3");
        play(game, "g8f6");
        play(game, "f3g1");
        play(game, "f6g8");

        // Assert
        assertTrue(game.isOver());

        GameResult result =
                game.result().orElseThrow();

        assertEquals(
                Outcome.DRAW,
                result.outcome()
        );

        assertEquals(
                "Triple repetición",
                result.reason()
        );

        // Los dos ciclos completos requieren ocho movimientos.
        assertEquals(
                8,
                game.history().size()
        );
    }

    @Test
    void aGameWithMovesAvailableIsNotOver() {
        Game game = StandardChess.newGame();

        assertFalse(game.isOver());

        assertEquals(
                Optional.empty(),
                game.result()
        );
    }

    // ---- promoción -------------------------------------------------------------------------

    @Test
    void promotingRequiresChoosingAPiece() {
        Game game = StandardChess.newGame(
                "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
        );

        IllegalMoveException e = assertThrows(
                IllegalMoveException.class,
                () -> game.play(
                        sq("a7"),
                        sq("a8")
                )
        );

        assertTrue(
                e.getMessage().contains("promoc")
        );

        assertEquals(
                0,
                game.history().size()
        );
    }

    @Test
    void promotingToAChosenPiece() {
        Game game = StandardChess.newGame(
                "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
        );

        Move played =
                play(game, "a7a8q");

        assertEquals(
                "a7a8q",
                played.notation()
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.QUEEN,
                                Color.WHITE
                        )
                ),
                game.position().pieceAt(
                        sq("a8")
                )
        );
    }

    @Test
    void promotingToAnInvalidPieceIsRejected() {
        Game game = StandardChess.newGame(
                "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
        );

        // No se puede promocionar a rey.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "a7a8k")
        );

        // Tampoco se puede promocionar a peón.
        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "a7a8p")
        );
    }

    @Test
    void promotionLetterIsRejectedWhenTheMoveIsNotAPromotion() {
        Game game = StandardChess.newGame();

        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "e2e4q")
        );
    }

    // ---- historial y observadores ----------------------------------------------------------

    @Test
    void historyKeepsMovesInOrder() {
        Game game = StandardChess.newGame();

        playAll(
                game,
                "e2e4 e7e5 g1f3"
        );

        assertEquals(
                List.of(
                        "e2e4",
                        "e7e5",
                        "g1f3"
                ),
                game.history()
                        .stream()
                        .map(Move::notation)
                        .toList()
        );

        // El historial expuesto por Game es de solo lectura.
        assertThrows(
                UnsupportedOperationException.class,
                () -> game.history().clear()
        );
    }

    @Test
    void listenersAreNotifiedOfMovesAndOfTheEnd() {
        List<String> events =
                new ArrayList<>();

        Game game =
                StandardChess.newGame();

        game.addListener(
                new GameListener() {

                    @Override
                    public void onMovePlayed(
                            Game g,
                            Move move
                    ) {
                        events.add(
                                "move:" + move.notation()
                        );
                    }

                    @Override
                    public void onGameOver(
                            Game g,
                            GameResult result
                    ) {
                        events.add(
                                "over:" + result.outcome()
                        );
                    }
                }
        );

        playAll(
                game,
                "f2f3 e7e5 g2g4 d8h4"
        );

        assertEquals(
                List.of(
                        "move:f2f3",
                        "move:e7e5",
                        "move:g2g4",
                        "move:d8h4",
                        "over:BLACK_WINS"
                ),
                events
        );
    }

    @Test
    void failedMovesDoNotNotifyListeners() {
        List<String> events =
                new ArrayList<>();

        Game game =
                StandardChess.newGame();

        game.addListener(
                new GameListener() {

                    @Override
                    public void onMovePlayed(
                            Game g,
                            Move move
                    ) {
                        events.add(
                                move.notation()
                        );
                    }
                }
        );

        assertThrows(
                IllegalMoveException.class,
                () -> play(game, "e2e5")
        );

        assertEquals(
                List.of(),
                events
        );
    }

    @Test
    void legalMovesFromASquareAreFilteredByOrigin() {
        Game game =
                StandardChess.newGame();

        assertEquals(
                List.of(
                        "e2e3",
                        "e2e4"
                ),
                notations(
                        game.legalMovesFrom(
                                sq("e2")
                        )
                )
        );

        // Pieza del rival: todavía no es su turno.
        assertEquals(
                List.of(),
                game.legalMovesFrom(
                        sq("e7")
                )
        );

        // Casilla vacía.
        assertEquals(
                List.of(),
                game.legalMovesFrom(
                        sq("e4")
                )
        );
    }

    @Test
    void restoredGameKeepsPositionHistory() {
        // Arrange
        Game original = StandardChess.newGame();

        play(original, "g1f3");
        play(original, "g8f6");
        play(original, "f3g1");
        play(original, "f6g8");

        List<chess.core.model.Position> history =
                original.positionHistory();

        // Act
        Game restored = StandardChess.restoreGame(
                original.position(),
                history
        );

        // Assert
        assertEquals(
                original.position(),
                restored.position()
        );

        assertEquals(
                original.positionHistory(),
                restored.positionHistory()
        );

        assertEquals(
                5,
                restored.positionHistory().size()
        );

        // La posición inicial apareció dos veces,
        // así que todavía no hay triple repetición.
        assertFalse(restored.isOver());
    }

    @Test
    void restoredGameCanStillDetectThreefoldRepetition() {
        // Arrange
        Game original = StandardChess.newGame();

        // Primer ciclo completo:
        // la posición inicial aparece por segunda vez.
        play(original, "g1f3");
        play(original, "g8f6");
        play(original, "f3g1");
        play(original, "f6g8");

        assertFalse(original.isOver());

        // Simulamos que el backend se apagó y la partida
        // fue reconstruida desde información persistida.
        Game restored = StandardChess.restoreGame(
                original.position(),
                original.positionHistory()
        );

        // Act - completamos otro ciclo.
        play(restored, "g1f3");
        play(restored, "g8f6");
        play(restored, "f3g1");
        play(restored, "f6g8");

        // Assert
        assertTrue(restored.isOver());

        GameResult result =
                restored.result().orElseThrow();

        assertEquals(
                Outcome.DRAW,
                result.outcome()
        );

        assertEquals(
                "Triple repetición",
                result.reason()
        );
    }
}

