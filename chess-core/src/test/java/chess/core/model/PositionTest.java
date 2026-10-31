package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.move.CastlingMove;
import chess.core.move.DoublePushMove;
import chess.core.move.EnPassantMove;
import chess.core.move.PromotionMove;
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

    private static final Piece WHITE_ROOK =
            new Piece(StandardPieces.ROOK, Color.WHITE);

    private static final Piece BLACK_PAWN =
            new Piece(StandardPieces.PAWN, Color.BLACK);

    @Test
    void initialPositionHasThirtyTwoPiecesInTheRightPlaces() {
        Position start =
                StandardChess.initialPosition();

        assertEquals(
                32,
                start.occupied().cardinality()
        );

        assertEquals(
                16,
                start.occupiedBy(Color.WHITE).cardinality()
        );

        assertEquals(
                16,
                start.occupiedBy(Color.BLACK).cardinality()
        );

        assertEquals(
                8,
                start.pieces(
                        Color.WHITE,
                        StandardPieces.PAWN
                ).cardinality()
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.KING,
                                Color.WHITE
                        )
                ),
                start.pieceAt(sq("e1"))
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.QUEEN,
                                Color.BLACK
                        )
                ),
                start.pieceAt(sq("d8"))
        );

        assertEquals(
                Optional.empty(),
                start.pieceAt(sq("e4"))
        );

        assertEquals(
                Color.WHITE,
                start.sideToMove()
        );
    }

    @Test
    void builderPlacesRemovesAndRelocates() {
        Position.Builder builder =
                Position.builder(
                        BoardGeometry.STANDARD
                );

        builder.place(
                sq("a1"),
                WHITE_ROOK
        );

        builder.place(
                sq("a5"),
                BLACK_PAWN
        );

        assertEquals(
                Optional.of(WHITE_ROOK),
                builder.pieceAt(sq("a1"))
        );

        builder.relocate(
                sq("a1"),
                sq("a5")
        );

        Position position =
                builder.build();

        assertEquals(
                Optional.empty(),
                position.pieceAt(sq("a1"))
        );

        assertEquals(
                Optional.of(WHITE_ROOK),
                position.pieceAt(sq("a5"))
        );

        assertEquals(
                1,
                position.occupied().cardinality()
        );

        assertEquals(
                0,
                position.pieces(
                        Color.BLACK,
                        StandardPieces.PAWN
                ).cardinality()
        );

        Position.Builder again =
                position.toBuilder();

        again.remove(
                sq("a5")
        );

        assertEquals(
                0,
                again.build()
                        .occupied()
                        .cardinality()
        );

        assertThrows(
                IllegalStateException.class,
                () -> Position
                        .builder(BoardGeometry.STANDARD)
                        .relocate(
                                sq("a1"),
                                sq("a2")
                        )
        );
    }

    @Test
    void playingAMoveDoesNotModifyTheOriginalPosition() {
        Position start =
                StandardChess.initialPosition();

        Position after =
                start.play(
                        new StandardMove(
                                sq("e2"),
                                sq("e4")
                        )
                );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                ),
                start.pieceAt(sq("e2"))
        );

        assertEquals(
                Color.WHITE,
                start.sideToMove()
        );

        assertEquals(
                Optional.empty(),
                after.pieceAt(sq("e2"))
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                ),
                after.pieceAt(sq("e4"))
        );

        assertEquals(
                Color.BLACK,
                after.sideToMove()
        );
    }

    @Test
    void unmovedBitsAreClearedWhenAPieceMovesOrIsCaptured() {
        Position start =
                StandardChess.initialPosition();

        assertTrue(
                start.unmoved().has(
                        start.geometry()
                                .index(sq("h1"))
                )
        );

        Position moved =
                start.play(
                        new StandardMove(
                                sq("h1"),
                                sq("h3")
                        )
                );

        assertFalse(
                moved.unmoved().has(
                        moved.geometry()
                                .index(sq("h1"))
                )
        );

        assertFalse(
                moved.unmoved().has(
                        moved.geometry()
                                .index(sq("h3"))
                )
        );

        assertTrue(
                moved.unmoved().has(
                        moved.geometry()
                                .index(sq("a1"))
                )
        );
    }

    @Test
    void equalPositionsAreEqual() {
        Position a =
                StandardChess.initialPosition();

        Position b =
                position(
                        StandardChess.START_FEN
                );

        assertEquals(a, b);
        assertEquals(
                a.hashCode(),
                b.hashCode()
        );

        assertNotEquals(
                a,
                a.play(
                        new StandardMove(
                                sq("e2"),
                                sq("e4")
                        )
                )
        );
    }

    @Test
    void attackMapIncludesSquaresWithPiecesOfTheSameColor() {
        Position position =
                position(
                        "4k3/8/8/8/8/8/P7/R3K3 w - - 0 1"
                );

        assertTrue(
                position.attackedBy(Color.WHITE)
                        .has(
                                position.geometry()
                                        .index(sq("a2"))
                        )
        );

        assertTrue(
                position.attackedBy(Color.WHITE)
                        .has(
                                position.geometry()
                                        .index(sq("d1"))
                        )
        );

        assertFalse(
                position.attackedBy(Color.WHITE)
                        .has(
                                position.geometry()
                                        .index(sq("a3"))
                        )
        );
    }

    @Test
    void sidesWithoutARoyalPieceAreNeverInCheck() {
        Position position =
                position(
                        "8/8/8/8/8/8/8/R7 w - - 0 1"
                );

        assertFalse(
                position.isInCheck(Color.WHITE)
        );

        assertFalse(
                position.isInCheck(Color.BLACK)
        );
    }

    @Test
    void pieceTypesListsOnlyWhatIsOnTheBoard() {
        Position position =
                position(
                        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1"
                );

        assertEquals(
                2,
                position.pieceTypes().size()
        );

        assertTrue(
                position.pieceTypes()
                        .contains(StandardPieces.ROOK)
        );

        assertTrue(
                position.pieceTypes()
                        .contains(StandardPieces.KING)
        );

        assertEquals(
                Bitboard.EMPTY,
                position.pieces(
                        StandardPieces.QUEEN
                )
        );
    }

    // ---- contador de medios movimientos ----------------------------------------------------

    @Test
    void halfmoveClockDefaultsToZero() {
        Position position =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .build();

        assertEquals(
                0,
                position.halfmoveClock()
        );
    }

    @Test
    void builderCanSetHalfmoveClock() {
        Position position =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .halfmoveClock(37)
                        .build();

        assertEquals(
                37,
                position.halfmoveClock()
        );
    }

    @Test
    void toBuilderPreservesHalfmoveClock() {
        Position original =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .halfmoveClock(42)
                        .build();

        Position copy =
                original.toBuilder()
                        .build();

        assertEquals(
                42,
                copy.halfmoveClock()
        );
    }

    @Test
    void negativeHalfmoveClockIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Position
                        .builder(BoardGeometry.STANDARD)
                        .halfmoveClock(-1)
        );
    }

    @Test
    void normalMoveIncrementsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/5N2/4K3 w - - 17 1"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("f2"),
                                sq("g4")
                        )
                );

        assertEquals(
                18,
                next.halfmoveClock()
        );
    }

    @Test
    void pawnMoveResetsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/4P3/4K3 w - - 63 1"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("e2"),
                                sq("e3")
                        )
                );

        assertEquals(
                0,
                next.halfmoveClock()
        );
    }

    @Test
    void captureResetsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/5Nr1/4K3 w - - 42 1"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("f2"),
                                sq("g2")
                        )
                );

        assertEquals(
                0,
                next.halfmoveClock()
        );
    }

    @Test
    void doublePawnPushResetsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/4P3/4K3 w - - 35 1"
                );

        Position next =
                position.play(
                        new DoublePushMove(
                                sq("e2"),
                                sq("e4"),
                                sq("e3")
                        )
                );

        assertEquals(
                0,
                next.halfmoveClock()
        );

        assertEquals(
                Optional.of(sq("e3")),
                next.enPassantTarget()
        );
    }

    @Test
    void promotionResetsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/P7/8/8/8/8/8/4K3 w - - 71 1"
                );

        Position next =
                position.play(
                        new PromotionMove(
                                sq("a7"),
                                sq("a8"),
                                StandardPieces.QUEEN
                        )
                );

        assertEquals(
                0,
                next.halfmoveClock()
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.QUEEN,
                                Color.WHITE
                        )
                ),
                next.pieceAt(sq("a8"))
        );
    }

    @Test
    void enPassantResetsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 28 1"
                );

        Position next =
                position.play(
                        new EnPassantMove(
                                sq("e5"),
                                sq("d6"),
                                sq("d5")
                        )
                );

        assertEquals(
                0,
                next.halfmoveClock()
        );

        assertEquals(
                Optional.empty(),
                next.pieceAt(sq("d5"))
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.PAWN,
                                Color.WHITE
                        )
                ),
                next.pieceAt(sq("d6"))
        );
    }

    @Test
    void castlingIncrementsHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/8/4K2R w K - 23 1"
                );

        Position next =
                position.play(
                        new CastlingMove(
                                sq("e1"),
                                sq("g1"),
                                sq("h1"),
                                sq("f1")
                        )
                );

        assertEquals(
                24,
                next.halfmoveClock()
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.KING,
                                Color.WHITE
                        )
                ),
                next.pieceAt(sq("g1"))
        );

        assertEquals(
                Optional.of(
                        new Piece(
                                StandardPieces.ROOK,
                                Color.WHITE
                        )
                ),
                next.pieceAt(sq("f1"))
        );
    }

    @Test
    void formattingAfterNormalMoveWritesIncrementedHalfmoveClock() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/5N2/4K3 w - - 17 1"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("f2"),
                                sq("g4")
                        )
                );

        String formatted =
                StandardChess.fen()
                        .format(next);

        assertEquals(
                18,
                next.halfmoveClock()
        );

        assertEquals(
                "4k3/8/8/8/6N1/8/8/4K3 b - - 18 1",
                formatted
        );
    }

    // ---- número completo de jugada --------------------------------------------------------

    @Test
    void fullmoveNumberDefaultsToOne() {
        Position position =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .build();

        assertEquals(
                1,
                position.fullmoveNumber()
        );
    }

    @Test
    void builderCanSetFullmoveNumber() {
        Position position =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .fullmoveNumber(23)
                        .build();

        assertEquals(
                23,
                position.fullmoveNumber()
        );
    }

    @Test
    void toBuilderPreservesFullmoveNumber() {
        Position original =
                Position.builder(
                                BoardGeometry.STANDARD
                        )
                        .fullmoveNumber(42)
                        .build();

        Position copy =
                original.toBuilder()
                        .build();

        assertEquals(
                42,
                copy.fullmoveNumber()
        );
    }

    @Test
    void zeroFullmoveNumberIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Position
                        .builder(BoardGeometry.STANDARD)
                        .fullmoveNumber(0)
        );
    }

    @Test
    void negativeFullmoveNumberIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Position
                        .builder(BoardGeometry.STANDARD)
                        .fullmoveNumber(-1)
        );
    }

    @Test
    void whiteMoveDoesNotIncrementFullmoveNumber() {
        Position position =
                StandardChess.fen().parse(
                        "4k3/8/8/8/8/8/5N2/4K3 w - - 17 12"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("f2"),
                                sq("g4")
                        )
                );

        assertEquals(
                12,
                next.fullmoveNumber()
        );
    }

    @Test
    void blackMoveIncrementsFullmoveNumber() {
        Position position =
                StandardChess.fen().parse(
                        "4k1n1/8/8/8/8/8/8/4K3 b - - 17 12"
                );

        Position next =
                position.play(
                        new StandardMove(
                                sq("g8"),
                                sq("f6")
                        )
                );

        assertEquals(
                13,
                next.fullmoveNumber()
        );
    }

    @Test
    void fullmoveNumberIncrementsOnlyAfterBlackMoves() {
        Position initial =
                StandardChess.fen().parse(
                        "4k1n1/8/8/8/8/8/5N2/4K3 w - - 0 20"
                );

        Position afterWhite =
                initial.play(
                        new StandardMove(
                                sq("f2"),
                                sq("g4")
                        )
                );

        Position afterBlack =
                afterWhite.play(
                        new StandardMove(
                                sq("g8"),
                                sq("f6")
                        )
                );

        assertEquals(
                20,
                initial.fullmoveNumber()
        );

        assertEquals(
                20,
                afterWhite.fullmoveNumber()
        );

        assertEquals(
                21,
                afterBlack.fullmoveNumber()
        );
    }
}