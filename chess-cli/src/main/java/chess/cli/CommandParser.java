package chess.cli;

import chess.core.board.Square;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce una línea escrita por el usuario a un {@link Command}.
 *
 * <p>Formatos de jugada aceptados: {@code e2e4}, {@code e2 e4}, {@code e2-e4} y, para
 * promocionar, {@code e7e8q}.
 */
public final class CommandParser {

    private static final Pattern MOVE = Pattern.compile("^([a-z][0-9]{1,2})[ -]?([a-z][0-9]{1,2})([a-z])?$");

    public Command parse(String line) {
        String text = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return new Command.Empty();
        }
        switch (text) {
            case "salir", "quit", "exit", "q":
                return new Command.Quit();
            case "ayuda", "help", "?", "h":
                return new Command.Help();
            case "tablero", "board":
                return new Command.ShowBoard();
            case "fen":
                return new Command.ShowFen();
            default:
                break;
        }
        if (text.equals("jugadas") || text.equals("moves")) {
            return new Command.ListMoves(Optional.empty());
        }
        if (text.startsWith("jugadas ") || text.startsWith("moves ")) {
            String argument = text.substring(text.indexOf(' ') + 1).trim();
            try {
                return new Command.ListMoves(Optional.of(Square.parse(argument)));
            } catch (IllegalArgumentException e) {
                return new Command.Unknown(text, "'" + argument + "' no es una casilla válida (ejemplo: e2)");
            }
        }
        Matcher matcher = MOVE.matcher(text);
        if (matcher.matches()) {
            try {
                Square from = Square.parse(matcher.group(1));
                Square to = Square.parse(matcher.group(2));
                Optional<Character> promotion = Optional.ofNullable(matcher.group(3)).map(s -> s.charAt(0));
                return new Command.PlayMove(from, to, promotion);
            } catch (IllegalArgumentException e) {
                return new Command.Unknown(text, "casilla inválida en '" + text + "'");
            }
        }
        return new Command.Unknown(text, "no entiendo '" + text + "'. Escribí 'ayuda' para ver los comandos");
    }
}
