package chess.cli;

import chess.core.board.Square;

import java.util.Optional;

/**
 * Lo que el usuario puede pedir por consola. Es el resultado de interpretar una línea de texto
 * ({@link CommandParser}); {@link ConsoleSession} decide qué hacer con cada uno.
 */
public sealed interface Command {

    /** Mover de una casilla a otra; {@code promotion} es la letra de la pieza si se promociona. */
    record PlayMove(Square from, Square to, Optional<Character> promotion) implements Command {
    }

    /** Listar los movimientos legales, de toda la partida o de una casilla. */
    record ListMoves(Optional<Square> from) implements Command {
    }

    record ShowBoard() implements Command {
    }

    record ShowFen() implements Command {
    }

    record Help() implements Command {
    }

    record Quit() implements Command {
    }

    /** Línea vacía: se ignora. */
    record Empty() implements Command {
    }

    /** Texto que no se entendió, con el motivo para mostrarle al usuario. */
    record Unknown(String text, String reason) implements Command {
    }
}
