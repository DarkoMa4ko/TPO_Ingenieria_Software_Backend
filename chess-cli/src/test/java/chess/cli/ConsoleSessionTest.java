package chess.cli;

import chess.core.game.Game;
import chess.core.standard.StandardChess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de punta a punta del adaptador: se simula lo que el usuario escribe y se lee lo que imprime. */
class ConsoleSessionTest {

    private static String run(Game game, String... inputLines) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", inputLines)));
        new ConsoleSession(game, in, out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void showsTheBoardAndPromptsWhiteFirst() throws Exception {
        String output = run(StandardChess.newGame(), "salir");
        assertTrue(output.contains("8 r n b q k b n r 8"));
        assertTrue(output.contains("Blancas> "));
        assertTrue(output.contains("¡Hasta luego!"));
    }

    @Test
    void playsAMoveAndRedrawsTheBoardForBlack() throws Exception {
        String output = run(StandardChess.newGame(), "e2e4", "salir");
        assertTrue(output.contains("Jugada: e2e4"));
        assertTrue(output.contains("4 . . . . P . . . 4"));
        assertTrue(output.contains("Negras> "));
    }

    @Test
    @DisplayName("Mate del loco de punta a punta: anuncia jaque mate y gana el negro")
    void playsAFullGameUntilCheckmate() throws Exception {
        Game game = StandardChess.newGame();
        String output = run(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertTrue(output.contains("Jaque mate"));
        assertTrue(output.contains("Ganan las negras."));
        assertTrue(game.isOver());
        assertFalse(output.contains("Blancas> \n*")); // no vuelve a pedir jugada
    }

    @Test
    void announcesCheck() throws Exception {
        String output = run(StandardChess.newGame(), "e2e4", "f7f6", "d1h5", "salir");
        assertTrue(output.contains("¡Jaque a las negras!"));
    }

    @Test
    void explainsIllegalMovesAndKeepsAskingForTheSameColor() throws Exception {
        Game game = StandardChess.newGame();
        String output = run(game, "e2e5", "b1b3", "e7e5", "salir");
        assertTrue(output.contains("Movimiento inválido: Peón no puede moverse de e2 a e5"));
        assertTrue(output.contains("Movimiento inválido: Caballo no puede moverse de b1 a b3"));
        assertTrue(output.contains("Movimiento inválido: No hay ninguna pieza")
                || output.contains("Movimiento inválido: No es tu turno"));
        assertEquals(0, game.history().size());
    }

    @Test
    void rejectsWhatItDoesNotUnderstand() throws Exception {
        String output = run(StandardChess.newGame(), "hola", "salir");
        assertTrue(output.contains("Error: no entiendo 'hola'"));
    }

    @Test
    void listsLegalMoves() throws Exception {
        String output = run(StandardChess.newGame(), "jugadas e2", "jugadas e4", "jugadas", "salir");
        assertTrue(output.contains("2 movimiento(s): e2e3 e2e4"));
        assertTrue(output.contains("No hay movimientos legales desde e4."));
        assertTrue(output.contains("20 movimiento(s): "));
    }

    @Test
    void promotionNeedsALetterAndWorksWithIt() throws Exception {
        Game game = StandardChess.newGame("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        String output = run(game, "a7a8", "a7a8q", "salir");
        assertTrue(output.contains("Indicá la pieza de promoción"));
        assertTrue(output.contains("Jugada: a7a8q"));
        assertTrue(output.contains("8 Q . . . k . . . 8"));
    }

    @Test
    void helpAndFenCommands() throws Exception {
        String output = run(StandardChess.newGame(), "ayuda", "fen", "salir");
        assertTrue(output.contains("Comandos:"));
        assertTrue(output.contains(StandardChess.START_FEN));
    }

    @Test
    void endsGracefullyWhenTheInputEnds() throws Exception {
        String output = run(StandardChess.newGame(), "e2e4");
        assertTrue(output.contains("Jugada: e2e4"));
    }

    @Test
    void startsFromAFenGivenOnTheCommandLine() {
        Game game = ConsoleApp.createGame(new String[] {"--fen", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"});
        assertTrue(game.isOver());
        assertEquals(StandardChess.newGame().legalMoves().size(), ConsoleApp.createGame(new String[0]).legalMoves().size());
    }
}
