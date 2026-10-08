package chess.cli;

import chess.core.board.Square;
import chess.core.game.Game;
import chess.core.game.IllegalMoveException;
import chess.core.model.Move;
import chess.core.standard.StandardChess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bucle interactivo de la partida por consola. Es un <i>adaptador</i>: traduce texto del usuario
 * a llamadas sobre {@link Game} y muestra lo que el núcleo responde.
 *
 * <p>Recibe por constructor el juego, la entrada y la salida, así que se puede probar con texto
 * simulado sin tocar la consola real.
 */
public final class ConsoleSession {

    private final Game game;
    private final BufferedReader in;
    private final PrintStream out;
    private final CommandParser parser = new CommandParser();
    private final ConsoleView view;

    public ConsoleSession(Game game, BufferedReader in, PrintStream out) {
        this.game = game;
        this.in = in;
        this.out = out;
        this.view = new ConsoleView(out, new BoardRenderer());
        game.addListener(view);
    }

    public void run() {
        out.println("=== Ajedrez ===  (escribí 'ayuda' para ver los comandos)");
        view.show(game);
        while (!game.isOver()) {
            out.print(ConsoleView.colorName(game.turn()).substring(0, 1).toUpperCase()
                    + ConsoleView.colorName(game.turn()).substring(1) + "> ");
            out.flush();
            String line = readLine();
            if (line == null) {
                out.println();
                return; // fin de la entrada
            }
            if (!execute(parser.parse(line))) {
                return;
            }
        }
    }

    /** @return {@code false} si hay que terminar el programa */
    private boolean execute(Command command) {
        if (command instanceof Command.Quit) {
            out.println("¡Hasta luego!");
            return false;
        }
        if (command instanceof Command.Help) {
            printHelp();
        } else if (command instanceof Command.ShowBoard) {
            view.show(game);
        } else if (command instanceof Command.ShowFen) {
            out.println(StandardChess.fen().format(game.position()));
        } else if (command instanceof Command.ListMoves list) {
            printMoves(list.from().map(game::legalMovesFrom).orElseGet(game::legalMoves), list.from());
        } else if (command instanceof Command.PlayMove play) {
            tryMove(play);
        } else if (command instanceof Command.Unknown unknown) {
            out.println("Error: " + unknown.reason());
        }
        return true;
    }

    private void tryMove(Command.PlayMove play) {
        try {
            if (play.promotion().isPresent()) {
                game.play(play.from(), play.to(), play.promotion().get());
            } else {
                game.play(play.from(), play.to());
            }
        } catch (IllegalMoveException e) {
            out.println("Movimiento inválido: " + e.getMessage());
        }
    }

    private void printMoves(List<Move> moves, java.util.Optional<Square> from) {
        if (moves.isEmpty()) {
            out.println(from.isPresent()
                    ? "No hay movimientos legales desde " + from.get() + "."
                    : "No hay movimientos legales.");
            return;
        }
        out.println(moves.size() + " movimiento(s): "
                + moves.stream().map(Move::notation).sorted().collect(Collectors.joining(" ")));
    }

    private void printHelp() {
        out.println("Comandos:");
        out.println("  e2e4          mover de e2 a e4 (también 'e2 e4' o 'e2-e4')");
        out.println("  e7e8q         promocionar (q = reina, r = torre, b = alfil, n = caballo)");
        out.println("  jugadas       listar todos los movimientos legales");
        out.println("  jugadas e2    listar los movimientos legales de la pieza en e2");
        out.println("  tablero       volver a mostrar el tablero");
        out.println("  fen           mostrar la posición en notación FEN");
        out.println("  salir         terminar el programa");
        out.println("El enroque se juega moviendo el rey dos casillas (e1g1).");
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
