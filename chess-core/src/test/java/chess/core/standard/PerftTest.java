package chess.core.standard;

import chess.core.game.LegalMoveGenerator;
import chess.core.model.Move;
import chess.core.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "Perft": cuenta todas las posiciones alcanzables a cierta profundidad y compara con valores
 * publicados e independientes (chessprogramming.org, verificados además con la librería python-chess).
 * Si cualquier regla estuviera mal (enroque, al paso, promoción, clavadas, jaques) alguna cifra no coincidiría.
 */
class PerftTest {

    private static final LegalMoveGenerator GENERATOR = new LegalMoveGenerator();
    private static final Fen FEN = Fen.standard();

    private static long perft(Position position, int depth) {
        java.util.List<Move> moves = GENERATOR.legalMoves(position);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (Move move : moves) {
            nodes += perft(position.play(move), depth - 1);
        }
        return nodes;
    }

    private static void check(String fen, long... expectedByDepth) {
        Position position = FEN.parse(fen);
        for (int depth = 1; depth <= expectedByDepth.length; depth++) {
            assertEquals(expectedByDepth[depth - 1], perft(position, depth), "perft(" + depth + ") de " + fen);
        }
    }

    @Test
    @DisplayName("Posición inicial: 20, 400, 8.902, 197.281")
    void startingPosition() {
        check(StandardChess.START_FEN, 20, 400, 8902, 197281);
    }

    @Test
    @DisplayName("'Kiwipete': enroques, clavadas y capturas por todos lados")
    void kiwipete() {
        check("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 48, 2039, 97862);
    }

    @Test
    @DisplayName("Final de torres y peones: jaques descubiertos y captura al paso clavada")
    void rookEndgame() {
        check("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 14, 191, 2812, 43238);
    }

    @Test
    @DisplayName("Promociones con captura y enroques bloqueados por amenazas")
    void promotionsAndCastling() {
        check("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 6, 264, 9467);
    }

    @Test
    void positionFive() {
        check("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 44, 1486, 62379);
    }

    @Test
    void positionSix() {
        check("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10", 46, 2079, 89890);
    }
}
