package chess.core.model;

import chess.core.board.Bitboard;
import chess.core.board.BoardGeometry;
import chess.core.board.Square;
import chess.core.move.StandardMove;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import chess.core.move.CastlingMove;
import chess.core.move.DoublePushMove;
import chess.core.move.EnPassantMove;
import chess.core.move.PromotionMove;
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

    private static final Piece WHITE_ROOK = new Piece(StandardPieces.ROOK, Color.WHITE);
    private static final Piece BLACK_PAWN = new Piece(StandardPieces.PAWN, Color.BLACK);

    @Test
    void initialPositionHasThirtyTwoPiecesInTheRightPlaces() {
        Position start = StandardChess.initialPosition();
        assertEquals(32, start.occupied().cardinality());
        assertEquals(16, start.occupiedBy(Color.WHITE).cardinality());
        assertEquals(16, start.occupiedBy(Color.BLACK).cardinality());
        assertEquals(8, start.pieces(Color.WHITE, StandardPieces.PAWN).cardinality());
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.WHITE)), start.pieceAt(sq("e1")));
        assertEquals(Optional.of(new Piece(StandardPieces.QUEEN, Color.BLACK)), start.pieceAt(sq("d8")));
        assertEquals(Optional.empty(), start.pieceAt(sq("e4")));
        assertEquals(Color.WHITE, start.sideToMove());
    }

    @Test
    void builderPlacesRemovesAndRelocates() {
        Position.Builder b = Position.builder(BoardGeometry.STANDARD);
        b.place(sq("a1"), WHITE_ROOK);
        b.place(sq("a5"), BLACK_PAWN);
        assertEquals(Optional.of(WHITE_ROOK), b.pieceAt(sq("a1")));

        b.relocate(sq("a1"), sq("a5")); // captura el peón
        Position p = b.build();
        assertEquals(Optional.empty(), p.pieceAt(sq("a1")));
        assertEquals(Optional.of(WHITE_ROOK), p.pieceAt(sq("a5")));
        assertEquals(1, p.occupied().cardinality());
        assertEquals(0, p.pieces(Color.BLACK, StandardPieces.PAWN).cardinality());

        Position.Builder again = p.toBuilder();
        again.remove(sq("a5"));
        assertEquals(0, again.build().occupied().cardinality());
        assertThrows(IllegalStateException.class, () -> Position.builder(BoardGeometry.STANDARD).relocate(sq("a1"), sq("a2")));
    }

    @Test
    void playingAMoveDoesNotModifyTheOriginalPosition() {
        Position start = StandardChess.initialPosition();
        Position after = start.play(new StandardMove(sq("e2"), sq("e4")));

        assertEquals(Optional.of(new Piece(StandardPieces.PAWN, Color.WHITE)), start.pieceAt(sq("e2")));
        assertEquals(Color.WHITE, start.sideToMove());
        assertEquals(Optional.empty(), after.pieceAt(sq("e2")));
        assertEquals(Optional.of(new Piece(StandardPieces.PAWN, Color.WHITE)), after.pieceAt(sq("e4")));
        assertEquals(Color.BLACK, after.sideToMove());
    }

    @Test
    void unmovedBitsAreClearedWhenAPieceMovesOrIsCaptured() {
        Position start = StandardChess.initialPosition();
        assertTrue(start.unmoved().has(start.geometry().index(sq("h1"))));
        Position moved = start.play(new StandardMove(sq("h1"), sq("h3")));
        assertFalse(moved.unmoved().has(moved.geometry().index(sq("h1"))));
        assertFalse(moved.unmoved().has(moved.geometry().index(sq("h3"))));
        assertTrue(moved.unmoved().has(moved.geometry().index(sq("a1")))); // la otra torre sigue sin moverse
    }

    @Test
    void equalPositionsAreEqual() {
        Position a = StandardChess.initialPosition();
        Position b = position(StandardChess.START_FEN);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, a.play(new StandardMove(sq("e2"), sq("e4"))));
    }

    @Test
    void attackMapIncludesSquaresWithPiecesOfTheSameColor() {
        // La torre "protege" a su propio peón: esa casilla figura como atacada.
        Position p = position("4k3/8/8/8/8/8/P7/R3K3 w - - 0 1");
        assertTrue(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("a2"))));
        assertTrue(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("d1"))));
        assertFalse(p.attackedBy(Color.WHITE).has(p.geometry().index(sq("a3")))); // detrás de su propio peón
    }

    @Test
    void sidesWithoutARoyalPieceAreNeverInCheck() {
        Position p = position("8/8/8/8/8/8/8/R7 w - - 0 1");
        assertFalse(p.isInCheck(Color.WHITE));
        assertFalse(p.isInCheck(Color.BLACK));
    }

    @Test
    void pieceTypesListsOnlyWhatIsOnTheBoard() {
        Position p = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        assertEquals(2, p.pieceTypes().size());
        assertTrue(p.pieceTypes().contains(StandardPieces.ROOK));
        assertTrue(p.pieceTypes().contains(StandardPieces.KING));
        assertEquals(Bitboard.EMPTY, p.pieces(StandardPieces.QUEEN));
    }

    @Test
    void halfmoveClockDefaultsToZero() {
        // Arrange / Act
        Position position = Position.builder(BoardGeometry.STANDARD)
                .build();

        // Assert
        assertEquals(0, position.halfmoveClock());
    }

    @Test
    void builderCanSetHalfmoveClock() {
        // Arrange / Act
        Position position = Position.builder(BoardGeometry.STANDARD)
                .halfmoveClock(37)
                .build();

        // Assert
        assertEquals(37, position.halfmoveClock());
    }

    @Test
    void toBuilderPreservesHalfmoveClock() {
        // Arrange
        Position original = Position.builder(BoardGeometry.STANDARD)
                .halfmoveClock(42)
                .build();

        // Act
        Position copy = original.toBuilder().build();

        // Assert
        assertEquals(42, copy.halfmoveClock());
    }

    @Test
    void negativeHalfmoveClockIsRejected() {
        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> Position.builder(BoardGeometry.STANDARD)
                        .halfmoveClock(-1)
        );
    }

    @Test
    void normalMoveIncrementsHalfmoveClock() {
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/5N2/4K3 w - - 17 1"
        );

        // Act
        Position next = position.play(
                new StandardMove(
                        sq("f2"),
                        sq("g4")
                )
        );

        // Assert
        assertEquals(
                18,
                next.halfmoveClock()
        );
    }

    @Test
    void pawnMoveResetsHalfmoveClock() {
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/4P3/4K3 w - - 63 1"
        );

        // Act
        Position next = position.play(
                new StandardMove(
                        sq("e2"),
                        sq("e3")
                )
        );

        // Assert
        assertEquals(
                0,
                next.halfmoveClock()
        );
    }

    @Test
    void captureResetsHalfmoveClock() {
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/5Nr1/4K3 w - - 42 1"
        );

        // Act
        Position next = position.play(
                new StandardMove(
                        sq("f2"),
                        sq("g2")
                )
        );

        // Assert
        assertEquals(
                0,
                next.halfmoveClock()
        );
    }

    @Test
    void doublePawnPushResetsHalfmoveClock() {
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/4P3/4K3 w - - 35 1"
        );

        // Act
        Position next = position.play(
                new DoublePushMove(
                        sq("e2"),
                        sq("e4"),
                        sq("e3")
                )
        );

        // Assert
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
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/P7/8/8/8/8/8/4K3 w - - 71 1"
        );

        // Act
        Position next = position.play(
                new PromotionMove(
                        sq("a7"),
                        sq("a8"),
                        StandardPieces.QUEEN
                )
        );

        // Assert
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
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 28 1"
        );

        // Act
        Position next = position.play(
                new EnPassantMove(
                        sq("e5"),
                        sq("d6"),
                        sq("d5")
                )
        );

        // Assert
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
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/8/4K2R w K - 23 1"
        );

        // Act
        Position next = position.play(
                new CastlingMove(
                        sq("e1"),
                        sq("g1"),
                        sq("h1"),
                        sq("f1")
                )
        );

        // Assert
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
        // Arrange
        Position position = StandardChess.fen().parse(
                "4k3/8/8/8/8/8/5N2/4K3 w - - 17 1"
        );

        // Act
        Position next = position.play(
                new StandardMove(
                        sq("f2"),
                        sq("g4")
                )
        );

        String formatted =
                StandardChess.fen().format(next);

        // Assert
        assertEquals(
                18,
                next.halfmoveClock()
        );

        assertEquals(
                "4k3/8/8/8/6N1/8/8/4K3 b - - 18 1",
                formatted
        );
    }
}
