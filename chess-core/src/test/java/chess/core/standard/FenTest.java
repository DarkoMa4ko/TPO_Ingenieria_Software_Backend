package chess.core.standard;

import chess.core.board.BoardGeometry;
import chess.core.model.Color;
import chess.core.model.Piece;
import chess.core.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static chess.core.TestSupport.sq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FenTest {

    private final Fen fen = Fen.standard();

    @Test
    void startingPositionRoundTrips() {
        assertEquals(StandardChess.START_FEN, fen.format(fen.parse(StandardChess.START_FEN)));
    }

    @Test
    void variousPositionsRoundTrip() {
        for (String text : List.of(
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 0 1",
                "4k3/8/8/8/8/8/8/R3K2R b Q - 0 1")) {
            assertEquals(text, fen.format(fen.parse(text)));
        }
    }

    @Test
    void parsesPiecesAndSideToMove() {
        Position p = fen.parse("4k3/8/8/8/8/8/8/R3K3 b - - 0 1");
        assertEquals(Color.BLACK, p.sideToMove());
        assertEquals(Optional.of(new Piece(StandardPieces.ROOK, Color.WHITE)), p.pieceAt(sq("a1")));
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.BLACK)), p.pieceAt(sq("e8")));
        assertEquals(BoardGeometry.STANDARD, p.geometry());
    }

    @Test
    void afterTheFirstMoveTheFenReportsTheEnPassantSquare() {
        chess.core.game.Game game = StandardChess.newGame();
        game.play(sq("e2"), sq("e4"));
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", fen.format(game.position()));
    }

    @Test
    void castlingRightsAreDroppedWhenTheRookIsMissing() {
        // Dice "KQ" pero solo hay torre en h1: solo queda el derecho corto.
        assertEquals("4k3/8/8/8/8/8/8/4K2R w K - 0 1", fen.format(fen.parse("4k3/8/8/8/8/8/8/4K2R w KQ - 0 1")));
    }

    @Test
    void missingTrailingFieldsUseDefaults() {
        Position p = fen.parse("8/8/8/8/8/8/8/8");
        assertEquals(Color.WHITE, p.sideToMove());
        assertEquals(0, p.occupied().cardinality());
    }

    @Test
    void sizeOfTheBoardIsDeducedFromTheText() {
        Position wide = fen.parse("10/10/10/10/10/10/10/10 w - - 0 1");
        assertEquals(BoardGeometry.of(10, 8), wide.geometry());
        Position small = fen.parse("6/6/6/6/6/6 w - - 0 1");
        assertEquals(BoardGeometry.of(6, 6), small.geometry());
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> fen.parse(""));
        assertThrows(IllegalArgumentException.class, () -> fen.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> fen.parse(null));
        assertThrows(IllegalArgumentException.class, () -> fen.parse("4x3/8/8/8/8/8/8/8 w - - 0 1"));      // pieza desconocida
        assertThrows(IllegalArgumentException.class, () -> fen.parse("4k3/8/8/8/8/8/8/7 w - - 0 1"));      // fila corta
        assertThrows(IllegalArgumentException.class, () -> fen.parse("4k4/8/8/8/8/8/8/8 w - - 0 1"));      // fila larga
        assertThrows(IllegalArgumentException.class, () -> fen.parse("8/8/8/8/8/8/8/8 x - - 0 1"));         // turno inválido
        assertThrows(IllegalArgumentException.class, () -> fen.parse("8/8/8/8/8/8/8/8 w Z - 0 1"));         // enroque inválido
        assertThrows(IllegalArgumentException.class, () -> fen.parse("8/8/8/8/8/8/8/8 w - z9 0 1"));       // casilla inválida
    }

    @Test
    void aVariantWithoutCastlingRejectsCastlingRights() {
        Fen noCastling = new Fen(StandardPieces.ALL, List.of());
        assertThrows(IllegalArgumentException.class, () -> noCastling.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"));
        assertEquals("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1",
                noCastling.format(noCastling.parse("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1")));
    }
}
