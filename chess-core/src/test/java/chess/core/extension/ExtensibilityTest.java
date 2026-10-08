package chess.core.extension;

import chess.core.board.BoardGeometry;
import chess.core.board.Direction;
import chess.core.game.EndCondition;
import chess.core.game.Game;
import chess.core.game.GameResult;
import chess.core.game.LegalMoveGenerator;
import chess.core.game.Outcome;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Piece;
import chess.core.model.PieceType;
import chess.core.model.Position;
import chess.core.movement.CompositeMovement;
import chess.core.movement.LeapingMovement;
import chess.core.movement.PawnMovement;
import chess.core.standard.Fen;
import chess.core.standard.StandardChess;
import chess.core.standard.StandardPieces;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static chess.core.TestSupport.play;
import static chess.core.TestSupport.sq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la arquitectura: se agregan piezas, tableros y reglas de fin de partida
 * <b>sin modificar ninguna clase del núcleo</b>. Todo lo que se define acá es código de
 * "usuario" del núcleo (este es el tipo de consigna que puede aparecer en la defensa).
 */
class ExtensibilityTest {

    // ---- piezas nuevas, definidas únicamente por composición de reglas existentes -----------------

    /** Canciller = torre + caballo. */
    static final PieceType CHANCELLOR = new PieceType("Canciller", 'C',
            new CompositeMovement(StandardPieces.ROOK.movement(), StandardPieces.KNIGHT.movement()));

    /** Arzobispo = alfil + caballo. */
    static final PieceType ARCHBISHOP = new PieceType("Arzobispo", 'A',
            new CompositeMovement(StandardPieces.BISHOP.movement(), StandardPieces.KNIGHT.movement()));

    /** Camello: saltador (1,3). Una pieza "de hada" con una sola línea de configuración. */
    static final PieceType CAMEL = new PieceType("Camello", 'M',
            new LeapingMovement(Direction.symmetries(1, 3)));

    private static final LegalMoveGenerator GENERATOR = new LegalMoveGenerator();

    private static int movesOf(PieceType type, BoardGeometry geometry, String square) {
        Position position = Position.builder(geometry)
                .place(sq(square), new Piece(type, Color.WHITE))
                .build();
        return GENERATOR.legalMoves(position).size();
    }

    @Test
    void newPiecesWorkOnTheStandardBoard() {
        assertEquals(14 + 8, movesOf(CHANCELLOR, BoardGeometry.STANDARD, "d4"));  // torre + caballo
        assertEquals(13 + 8, movesOf(ARCHBISHOP, BoardGeometry.STANDARD, "d4"));  // alfil + caballo
        assertEquals(8, movesOf(CAMEL, BoardGeometry.STANDARD, "d4"));
        assertEquals(2, movesOf(CAMEL, BoardGeometry.STANDARD, "a1"));            // b4 y d2
    }

    @Test
    @DisplayName("Cambiar el tamaño del tablero a 10x8: las mismas reglas, sin 'wrap-around'")
    void sameRulesOnATenByEightBoard() {
        BoardGeometry wide = BoardGeometry.of(10, 8);
        assertEquals(16 + 2, movesOf(CHANCELLOR, wide, "a1")); // 9 + 7 de torre, 2 de caballo (b3, c2)
        assertEquals(16 + 2, movesOf(CHANCELLOR, wide, "j1")); // espejo en la otra esquina
        assertEquals(16 + 8, movesOf(CHANCELLOR, wide, "e4")); // 7 + 9 de torre, 8 de caballo
    }

    @Test
    void sameRulesOnASmallSixBySixBoard() {
        BoardGeometry small = BoardGeometry.of(6, 6);
        assertEquals(15, movesOf(StandardPieces.QUEEN, small, "a1"));
        assertEquals(19, movesOf(StandardPieces.QUEEN, small, "c3"));
        assertEquals(2, movesOf(StandardPieces.KNIGHT, small, "a1"));
    }

    // ---- una variante completa: ajedrez de Capablanca (10x8, con Arzobispo y Canciller) --------------

    /** Peón que además puede coronar Arzobispo o Canciller. */
    static final PieceType CAPABLANCA_PAWN = new PieceType("Peón", 'P', new PawnMovement(List.of(
            StandardPieces.QUEEN, StandardPieces.ROOK, StandardPieces.BISHOP, StandardPieces.KNIGHT,
            ARCHBISHOP, CHANCELLOR)));

    private static final Fen CAPABLANCA_FEN = new Fen(List.of(
            CAPABLANCA_PAWN, StandardPieces.KNIGHT, StandardPieces.BISHOP, StandardPieces.ROOK,
            StandardPieces.QUEEN, StandardPieces.KING, ARCHBISHOP, CHANCELLOR), List.of());

    private static Game capablancaGame(String fen) {
        return new Game(CAPABLANCA_FEN.parse(fen), new LegalMoveGenerator(), StandardChess.endConditions());
    }

    private static final String CAPABLANCA_START =
            "rnabqkbcnr/pppppppppp/10/10/10/10/PPPPPPPPPP/RNABQKBCNR w - - 0 1";

    @Test
    @DisplayName("Capablanca: 28 jugadas legales iniciales y 784 posiciones a profundidad 2")
    void capablancaOpening() {
        Position start = CAPABLANCA_FEN.parse(CAPABLANCA_START);
        assertEquals(BoardGeometry.of(10, 8), start.geometry());
        // 10 peones x 2 + 2 caballos x 2 + Arzobispo x 2 + Canciller x 2 = 28
        List<Move> first = GENERATOR.legalMoves(start);
        assertEquals(28, first.size());
        long nodes = 0;
        for (Move move : first) {
            nodes += GENERATOR.legalMoves(start.play(move)).size();
        }
        assertEquals(784, nodes);
    }

    @Test
    void capablancaGameIsPlayableEndToEnd() {
        Game game = capablancaGame(CAPABLANCA_START);
        play(game, "j2j4");                       // avance doble en la columna j (la nueva)
        play(game, "a7a5");
        play(game, "i1j3");                       // caballo hacia la columna j
        assertEquals(Color.BLACK, game.turn());
        assertEquals(Optional.of(new Piece(StandardPieces.KNIGHT, Color.WHITE)), game.position().pieceAt(sq("j3")));
    }

    @Test
    void promotionOptionsAreConfigurable() {
        Game game = capablancaGame("4k5/P9/10/10/10/10/10/4K5 w - - 0 1");
        List<String> promotions = game.legalMovesFrom(sq("a7")).stream().map(Move::notation).sorted().toList();
        assertEquals(List.of("a7a8a", "a7a8b", "a7a8c", "a7a8n", "a7a8q", "a7a8r"), promotions);
        play(game, "a7a8c");
        assertEquals(Optional.of(new Piece(CHANCELLOR, Color.WHITE)), game.position().pieceAt(sq("a8")));
    }

    // ---- una regla de fin de partida nueva, sin tocar Game ------------------------------------------

    /** Si solo quedan los dos reyes nadie puede dar mate: tablas. */
    static final class InsufficientMaterial implements EndCondition {
        @Override
        public Optional<GameResult> evaluate(Position position, List<Move> legalMoves) {
            return position.occupied().cardinality() == 2
                    ? Optional.of(GameResult.draw("Material insuficiente"))
                    : Optional.empty();
        }
    }

    @Test
    void aNewEndConditionPlugsIntoGame() {
        List<EndCondition> rules = new ArrayList<>(StandardChess.endConditions());
        rules.add(new InsufficientMaterial());
        Game game = new Game(StandardChess.fen().parse("4k3/8/8/8/8/8/4p3/4K3 w - - 0 1"),
                new LegalMoveGenerator(), rules);
        assertEquals(Optional.empty(), game.result());
        play(game, "e1e2");                      // el rey captura el último peón
        GameResult result = game.result().orElseThrow();
        assertEquals(Outcome.DRAW, result.outcome());
        assertTrue(result.reason().contains("Material"));
    }
}
