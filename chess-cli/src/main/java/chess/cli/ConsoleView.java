package chess.cli;

import chess.core.game.Game;
import chess.core.game.GameListener;
import chess.core.game.GameResult;
import chess.core.model.Color;
import chess.core.model.Move;

import java.io.PrintStream;

/**
 * Vista de consola: es un observador del juego (implementa el puerto {@link GameListener} que
 * define el núcleo). Cada vez que se juega un movimiento redibuja el tablero y avisa del jaque
 * o del final de la partida.
 */
public final class ConsoleView implements GameListener {

    private final PrintStream out;
    private final BoardRenderer renderer;

    public ConsoleView(PrintStream out, BoardRenderer renderer) {
        this.out = out;
        this.renderer = renderer;
    }

    /** Muestra el estado actual: tablero y de quién es el turno. */
    public void show(Game game) {
        out.println();
        out.println(renderer.render(game.position()));
        out.println();
    }

    @Override
    public void onMovePlayed(Game game, Move move) {
        out.println();
        out.println("Jugada: " + move.notation());
        show(game);
        if (game.isInCheck() && !game.isOver()) {
            out.println("¡Jaque a las " + colorName(game.turn()) + "!");
        }
    }

    @Override
    public void onGameOver(Game game, GameResult result) {
        out.println("*** Fin de la partida: " + result.reason() + " ***");
        switch (result.outcome()) {
            case WHITE_WINS:
                out.println("Ganan las blancas.");
                break;
            case BLACK_WINS:
                out.println("Ganan las negras.");
                break;
            default:
                out.println("Empate.");
                break;
        }
    }

    static String colorName(Color color) {
        return color == Color.WHITE ? "blancas" : "negras";
    }
}
