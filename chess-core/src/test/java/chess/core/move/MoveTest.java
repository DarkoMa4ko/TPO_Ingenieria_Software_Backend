package chess.core.move;

import chess.core.board.BoardGeometry;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Piece;
import chess.core.model.Position;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static chess.core.TestSupport.position;
import static chess.core.TestSupport.sq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MoveTest {

    private static final Piece WHITE_PAWN = new Piece(StandardPieces.PAWN, Color.WHITE);
    private static final Piece BLACK_PAWN = new Piece(StandardPieces.PAWN, Color.BLACK);

    @Test
    void standardMoveRelocatesAndCaptures() {
        Position before = position("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1");
        Position after = before.play(new StandardMove(sq("e4"), sq("d5")));
        assertEquals(Optional.of(WHITE_PAWN), after.pieceAt(sq("d5")));
        assertEquals(Optional.empty(), after.pieceAt(sq("e4")));
        assertEquals(0, after.pieces(Color.BLACK, StandardPieces.PAWN).cardinality());
    }

    @Test
    void doublePushLeavesTheEnPassantTargetForExactlyOneTurn() {
        Position start = position("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1");
        Position afterPush = start.play(new DoublePushMove(sq("e2"), sq("e4"), sq("e3")));
        assertEquals(Optional.of(sq("e3")), afterPush.enPassantTarget());
        Position afterReply = afterPush.play(new StandardMove(sq("e8"), sq("d8")));
        assertEquals(Optional.empty(), afterReply.enPassantTarget());
    }

    @Test
    void enPassantRemovesThePawnBesideTheOrigin() {
        Position before = position("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        Position after = before.play(new EnPassantMove(sq("e5"), sq("d6"), sq("d5")));
        assertEquals(Optional.of(WHITE_PAWN), after.pieceAt(sq("d6")));
        assertEquals(Optional.empty(), after.pieceAt(sq("d5")));
        assertEquals(Optional.empty(), after.pieceAt(sq("e5")));
    }

    @Test
    void promotionReplacesThePawnKeepingItsColor() {
        Position before = position("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        Move move = new PromotionMove(sq("a7"), sq("a8"), StandardPieces.KNIGHT);
        Position after = before.play(move);
        assertEquals(Optional.of(new Piece(StandardPieces.KNIGHT, Color.WHITE)), after.pieceAt(sq("a8")));
        assertEquals(Optional.empty(), after.pieceAt(sq("a7")));
        assertEquals(Optional.of(StandardPieces.KNIGHT), move.promotion());
    }

    @Test
    void blackPromotionKeepsBlackColor() {
        Position before = position("4k3/8/8/8/8/8/p7/4K3 b - - 0 1");
        Position after = before.play(new PromotionMove(sq("a2"), sq("a1"), StandardPieces.QUEEN));
        assertEquals(Optional.of(new Piece(StandardPieces.QUEEN, Color.BLACK)), after.pieceAt(sq("a1")));
    }

    @Test
    void castlingMovesKingAndRook() {
        Position before = position("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        Position kingSide = before.play(new CastlingMove(sq("e1"), sq("g1"), sq("h1"), sq("f1")));
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.WHITE)), kingSide.pieceAt(sq("g1")));
        assertEquals(Optional.of(new Piece(StandardPieces.ROOK, Color.WHITE)), kingSide.pieceAt(sq("f1")));
        assertEquals(Optional.empty(), kingSide.pieceAt(sq("e1")));
        assertEquals(Optional.empty(), kingSide.pieceAt(sq("h1")));
    }

    @Test
    void notationUsesCoordinatesAndLowercasePromotionLetter() {
        assertEquals("e2e4", new StandardMove(sq("e2"), sq("e4")).notation());
        assertEquals("e2e4", new DoublePushMove(sq("e2"), sq("e4"), sq("e3")).notation());
        assertEquals("e7e8q", new PromotionMove(sq("e7"), sq("e8"), StandardPieces.QUEEN).notation());
        assertEquals("e1g1", new CastlingMove(sq("e1"), sq("g1"), sq("h1"), sq("f1")).notation());
    }

    @Test
    void movesAreComparedByValue() {
        assertEquals(new StandardMove(sq("a1"), sq("a2")), new StandardMove(sq("a1"), sq("a2")));
        assertEquals(new PromotionMove(sq("a7"), sq("a8"), StandardPieces.ROOK),
                new PromotionMove(sq("a7"), sq("a8"), StandardPieces.ROOK));
    }

    @Test
    void builderExposesTheBoardShape() {
        Position.Builder b = Position.builder(BoardGeometry.STANDARD);
        b.place(sq("b2"), BLACK_PAWN);
        assertEquals(BoardGeometry.STANDARD, b.geometry());
    }
}
