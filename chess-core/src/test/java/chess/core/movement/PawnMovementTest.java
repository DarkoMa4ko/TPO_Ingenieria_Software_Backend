package chess.core.movement;

import chess.core.board.BoardGeometry;
import chess.core.game.Game;
import chess.core.game.LegalMoveGenerator;
import chess.core.model.Color;
import chess.core.model.Piece;
import chess.core.model.Position;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static chess.core.TestSupport.legalMovesFrom;
import static chess.core.TestSupport.notationsFrom;
import static chess.core.TestSupport.play;
import static chess.core.TestSupport.playAll;
import static chess.core.TestSupport.sq;
import static chess.core.TestSupport.targets;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PawnMovementTest {

    @Test
    void pawnAdvancesOneOrTwoSquaresFromItsStartRank() {
        assertEquals(
                List.of("e3", "e4"),
                targets(
                        "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
                        "e2"
                )
        );

        assertEquals(
                List.of("e5"),
                targets(
                        "4k3/8/8/8/4P3/8/8/4K3 w - - 0 1",
                        "e4"
                )
        );
    }

    @Test
    void blackPawnsAdvanceDownTheBoard() {
        assertEquals(
                List.of("e5", "e6"),
                targets(
                        "4k3/4p3/8/8/8/8/8/4K3 b - - 0 1",
                        "e7"
                )
        );
    }

    @Test
    void pawnCannotAdvanceIntoAPiece() {
        assertEquals(
                List.of(),
                targets(
                        "4k3/8/8/8/8/4p3/4P3/4K3 w - - 0 1",
                        "e2"
                )
        );

        assertEquals(
                List.of("e3"),
                targets(
                        "4k3/8/8/8/4p3/8/4P3/4K3 w - - 0 1",
                        "e2"
                )
        );
    }

    @Test
    void pawnCapturesDiagonallyButNotStraight() {
        assertEquals(
                List.of(
                        "d5",
                        "e5",
                        "f5"
                ),
                targets(
                        "4k3/8/8/3p1p2/4P3/8/8/4K3 w - - 0 1",
                        "e4"
                )
        );

        assertEquals(
                List.of(),
                targets(
                        "4k3/8/8/4p3/4P3/8/8/4K3 w - - 0 1",
                        "e4"
                )
        );
    }

    @Test
    void pawnCannotMoveDiagonallyToAnEmptySquareOrCaptureOwnPieces() {
        assertEquals(
                List.of("e5"),
                targets(
                        "4k3/8/8/8/4P3/8/8/4K3 w - - 0 1",
                        "e4"
                )
        );

        assertEquals(
                List.of("e5"),
                targets(
                        "4k3/8/8/3P4/4P3/8/8/4K3 w - - 0 1",
                        "e4"
                )
        );
    }

    @Test
    @DisplayName(
            "Un peón en la columna a no captura 'dando la vuelta' hasta la columna h"
    )
    void pawnCapturesDoNotWrapAround() {
        assertEquals(
                List.of("a5"),
                targets(
                        "4k3/8/8/8/P6p/8/8/4K3 w - - 0 1",
                        "a4"
                )
        );

        assertEquals(
                List.of("h5"),
                targets(
                        "4k3/8/8/8/p6P/8/8/4K3 w - - 0 1",
                        "h4"
                )
        );
    }

    // ---- captura al paso -----------------------------------------------------------------

    @Test
    void enPassantIsOfferedRightAfterTheDoublePush() {
        assertEquals(
                List.of(
                        "d6",
                        "e6"
                ),
                targets(
                        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                        "e5"
                )
        );
    }

    @Test
    void enPassantRemovesTheCapturedPawn() {
        Game game =
                StandardChess.newGame(
                        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
                );

        play(
                game,
                "e5d6"
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                ),
                game.position()
                        .pieceAt(
                                sq("d6")
                        )
        );

        assertEquals(
                Optional.empty(),
                game.position()
                        .pieceAt(
                                sq("d5")
                        )
        );
    }

    @Test
    void enPassantExpiresIfNotUsedImmediately() {
        Game game =
                StandardChess.newGame();

        playAll(
                game,
                "e2e4 a7a6 e4e5 d7d5"
        );

        assertEquals(
                List.of(
                        "e5d6",
                        "e5e6"
                ),
                notationsFrom(
                        game,
                        "e5"
                )
        );

        playAll(
                game,
                "h2h3 h7h6"
        );

        assertEquals(
                List.of("e5e6"),
                notationsFrom(
                        game,
                        "e5"
                )
        );
    }

    @Test
    @DisplayName(
            "La captura al paso es ilegal si deja al propio rey en jaque (clavada horizontal)"
    )
    void enPassantIsIllegalWhenItExposesTheKing() {
        assertEquals(
                List.of("d5d6"),
                legalMovesFrom(
                        "8/8/8/K1pP3r/8/8/8/7k w - c6 0 1",
                        "d5"
                )
        );
    }

    // ---- regresión B0001 -----------------------------------------------------------------

    @Test
    void inventedEnPassantTargetDoesNotGenerateCapture() {
        Position position =
                baseEnPassantPosition()
                        .enPassantTarget(
                                sq("d6")
                        )
                        .build();

        assertEquals(
                List.of("e5e6"),
                movesFromE5(position)
        );
    }

    @Test
    void enPassantIsNotGeneratedAgainstANonPawnPiece() {
        Position position =
                baseEnPassantPosition()
                        .place(
                                sq("d5"),
                                new Piece(
                                        StandardPieces.ROOK,
                                        Color.BLACK
                                )
                        )
                        .enPassantTarget(
                                sq("d6")
                        )
                        .build();

        assertEquals(
                List.of("e5e6"),
                movesFromE5(position)
        );
    }

    @Test
    void defensiveValidationStillAllowsRealEnPassant() {
        Position position =
                baseEnPassantPosition()
                        .place(
                                sq("d5"),
                                new Piece(
                                        StandardPieces.PAWN,
                                        Color.BLACK
                                )
                        )
                        .enPassantTarget(
                                sq("d6")
                        )
                        .build();

        assertEquals(
                List.of(
                        "e5d6",
                        "e5e6"
                ),
                movesFromE5(position)
        );
    }

    private static Position.Builder baseEnPassantPosition() {
        return Position
                .builder(
                        BoardGeometry.STANDARD
                )
                .place(
                        sq("e5"),
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                )
                .place(
                        sq("e1"),
                        new Piece(
                                StandardPieces.KING,
                                Color.WHITE
                        )
                )
                .place(
                        sq("e8"),
                        new Piece(
                                StandardPieces.KING,
                                Color.BLACK
                        )
                )
                .sideToMove(
                        Color.WHITE
                );
    }

    private static List<String> movesFromE5(
            Position position
    ) {
        return new LegalMoveGenerator()
                .legalMoves(position)
                .stream()
                .filter(move ->
                        move.from().equals(
                                sq("e5")
                        )
                )
                .map(move ->
                        move.notation()
                )
                .sorted()
                .toList();
    }

    // ---- promoción -----------------------------------------------------------------------

    @Test
    void reachingTheLastRankOffersFourPromotions() {
        assertEquals(
                List.of(
                        "a7a8b",
                        "a7a8n",
                        "a7a8q",
                        "a7a8r"
                ),
                legalMovesFrom(
                        "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
                        "a7"
                )
        );
    }

    @Test
    void promotionCanAlsoCapture() {
        List<String> moves =
                legalMovesFrom(
                        "1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1",
                        "a7"
                );

        assertEquals(
                8,
                moves.size()
        );

        assertEquals(
                true,
                moves.contains("a7b8q")
        );

        assertEquals(
                true,
                moves.contains("a7a8n")
        );
    }

    @Test
    void blackPromotesOnTheFirstRank() {
        assertEquals(
                List.of(
                        "a2a1b",
                        "a2a1n",
                        "a2a1q",
                        "a2a1r"
                ),
                legalMovesFrom(
                        "4k3/8/8/8/8/8/p7/4K3 b - - 0 1",
                        "a2"
                )
        );
    }

    @Test
    void promotedPieceMovesLikeItsNewType() {
        Game game =
                StandardChess.newGame(
                        "7k/P7/8/8/8/8/8/K7 w - - 0 1"
                );

        play(
                game,
                "a7a8q"
        );

        play(
                game,
                "h8g7"
        );

        List<String> moves =
                notationsFrom(
                        game,
                        "a8"
                );

        assertEquals(
                true,
                moves.contains("a8h1")
        );

        assertEquals(
                true,
                moves.contains("a8a2")
        );

        assertEquals(
                true,
                moves.contains("a8h8")
        );
    }
}