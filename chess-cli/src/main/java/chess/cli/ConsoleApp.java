package chess.cli;

import chess.core.game.Game;
import chess.core.standard.StandardChess;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Punto de entrada: arma el juego (raíz de composición) y se lo entrega a la sesión de consola.
 *
 * <pre>
 *   java -jar ajedrez.jar                      partida nueva
 *   java -jar ajedrez.jar --fen "&lt;posición&gt;"   arranca desde una posición FEN
 * </pre>
 */
public final class ConsoleApp {

    private ConsoleApp() {
    }

    public static void main(String[] args) {
        Game game = createGame(args);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
        new ConsoleSession(game, in, System.out).run();
    }

    static Game createGame(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--fen")) {
                return StandardChess.newGame(args[i + 1]);
            }
        }
        return StandardChess.newGame();
    }
}
