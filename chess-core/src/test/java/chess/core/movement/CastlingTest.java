package chess.core.movement;

import chess.core.game.Game;
import chess.core.model.Color;
import chess.core.model.Piece;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static chess.core.TestSupport.play;
import static chess.core.TestSupport.playAll;
import static chess.core.TestSupport.sq;
import static chess.core.TestSupport.targets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastlingTest {

    private static final String BOTH_SIDES = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";

    @Test
    void whiteCanCastleOnBothSides() {
        assertEquals(List.of("c1", "d1", "d2", "e2", "f1", "f2", "g1"), targets(BOTH_SIDES, "e1"));
    }

    @Test
    void blackCanCastleOnBothSides() {
        assertEquals(List.of("c8", "d7", "d8", "e7", "f7", "f8", "g8"),
                targets("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8"));
    }

    @Test
    void castlingMovesTheRookToo() {
        Game game = StandardChess.newGame(BOTH_SIDES);
        play(game, "e1g1");
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.WHITE)), game.position().pieceAt(sq("g1")));
        assertEquals(Optional.of(new Piece(StandardPieces.ROOK, Color.WHITE)), game.position().pieceAt(sq("f1")));
        assertEquals(Optional.empty(), game.position().pieceAt(sq("h1")));

        play(game, "e8c8");
        assertEquals(Optional.of(new Piece(StandardPieces.KING, Color.BLACK)), game.position().pieceAt(sq("c8")));
        assertEquals(Optional.of(new Piece(StandardPieces.ROOK, Color.BLACK)), game.position().pieceAt(sq("d8")));
    }

    @Test
    void cannotCastleThroughPieces() {
        // Caballo propio en f1: sin enroque corto (y el rey tampoco puede ir a f1).
        assertEquals(List.of("c1", "d1", "d2", "e2", "f2"), targets("r3k2r/8/8/8/8/8/8/R3KN1R w KQkq - 0 1", "e1"));
        // Caballo en b1: sin enroque largo, aunque b1 no sea una casilla que cruce el rey.
        assertEquals(List.of("d1", "d2", "e2", "f1", "f2", "g1"), targets("r3k2r/8/8/8/8/8/8/RN2K2R w KQkq - 0 1", "e1"));
    }

    @Test
    @DisplayName("No se puede enrocar pasando por una casilla atacada")
    void cannotCastleThroughAttackedSquare() {
        // Torre negra en f8 ataca f1 (y f2): sin enroque corto; el largo sigue disponible.
        assertEquals(List.of("c1", "d1", "d2", "e2"), targets("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1", "e1"));
    }

    @Test
    void cannotCastleOutOfCheck() {
        // Torre negra en e2 da jaque: ni g1 ni c1.
        assertEquals(List.of("d1", "e2", "f1"), targets("4k3/8/8/8/8/8/4r3/R3K2R w KQ - 0 1", "e1"));
    }

    @Test
    @DisplayName("Sí se puede enrocar largo aunque la torre cruce una casilla atacada (b1)")
    void queenSideCastlingAllowedWhenOnlyTheRookSquareIsAttacked() {
        assertTrue(targets("1r2k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "e1").contains("c1"));
    }

    @Test
    void cannotCastleWithoutTheRook() {
        // Derecho de enroque declarado en el FEN, pero no hay torre en a1: no se concede.
        assertFalse(targets("4k3/8/8/8/8/8/8/4K2R w KQ - 0 1", "e1").contains("c1"));
        assertTrue(targets("4k3/8/8/8/8/8/8/4K2R w KQ - 0 1", "e1").contains("g1"));
    }

    @Test
    void noCastlingWithoutRights() {
        List<String> t = targets("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1", "e1");
        assertFalse(t.contains("g1"));
        assertFalse(t.contains("c1"));
        List<String> onlyKingSide = targets("r3k2r/8/8/8/8/8/8/R3K2R w K - 0 1", "e1");
        assertTrue(onlyKingSide.contains("g1"));
        assertFalse(onlyKingSide.contains("c1"));
    }

    @Test
    @DisplayName("Mover la torre pierde el derecho de ese lado, aunque después vuelva a su casilla")
    void movingTheRookForfeitsTheRightEvenIfItComesBack() {
        Game game = StandardChess.newGame(BOTH_SIDES);
        playAll(game, "h1g1 a8b8 g1h1 b8a8");
        List<String> t = game.legalMovesFrom(sq("e1")).stream().map(m -> m.to().toString()).sorted().toList();
        assertFalse(t.contains("g1"));   // el enroque corto se perdió
        assertTrue(t.contains("c1"));    // el largo sigue vivo
    }

    @Test
    void movingTheKingForfeitsBothRights() {
        Game game = StandardChess.newGame(BOTH_SIDES);
        playAll(game, "e1e2 a8b8 e2e1 b8a8");
        List<String> t = game.legalMovesFrom(sq("e1")).stream().map(m -> m.to().toString()).sorted().toList();
        assertFalse(t.contains("g1"));
        assertFalse(t.contains("c1"));
    }

    @Test
    void capturingARookRemovesThatRightFromTheOpponent() {
        Game game = StandardChess.newGame(BOTH_SIDES);
        play(game, "h1h8"); // la torre blanca captura la torre negra de h8
        // Blancas perdieron el enroque corto (movieron esa torre) y negras el corto (les capturaron la torre).
        assertEquals("r3k2R/8/8/8/8/8/8/R3K3 b Qq - 0 1", StandardChess.fen().format(game.position()));
    }
}
