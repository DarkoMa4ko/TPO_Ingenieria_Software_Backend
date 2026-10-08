package chess.cli;

import chess.core.board.Square;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    private static Square sq(String text) {
        return Square.parse(text);
    }

    @Test
    void parsesPlainMoves() {
        Command expected = new Command.PlayMove(sq("e2"), sq("e4"), Optional.empty());
        assertEquals(expected, parser.parse("e2e4"));
        assertEquals(expected, parser.parse("  E2E4  "));
        assertEquals(expected, parser.parse("e2 e4"));
        assertEquals(expected, parser.parse("e2-e4"));
    }

    @Test
    void parsesPromotions() {
        assertEquals(new Command.PlayMove(sq("e7"), sq("e8"), Optional.of('q')), parser.parse("e7e8q"));
        assertEquals(new Command.PlayMove(sq("a2"), sq("a1"), Optional.of('n')), parser.parse("a2a1N"));
    }

    @Test
    void parsesTheSimpleCommands() {
        assertEquals(new Command.Quit(), parser.parse("salir"));
        assertEquals(new Command.Quit(), parser.parse("QUIT"));
        assertEquals(new Command.Help(), parser.parse("ayuda"));
        assertEquals(new Command.Help(), parser.parse("?"));
        assertEquals(new Command.ShowBoard(), parser.parse("tablero"));
        assertEquals(new Command.ShowFen(), parser.parse("fen"));
        assertEquals(new Command.Empty(), parser.parse("   "));
        assertEquals(new Command.Empty(), parser.parse(null));
    }

    @Test
    void parsesMoveListings() {
        assertEquals(new Command.ListMoves(Optional.empty()), parser.parse("jugadas"));
        assertEquals(new Command.ListMoves(Optional.of(sq("e2"))), parser.parse("jugadas e2"));
        assertEquals(new Command.ListMoves(Optional.of(sq("g1"))), parser.parse("moves g1"));
    }

    @Test
    void reportsWhatItDoesNotUnderstand() {
        assertTrue(parser.parse("hola") instanceof Command.Unknown);
        assertTrue(parser.parse("e2") instanceof Command.Unknown);
        assertTrue(parser.parse("jugadas zz") instanceof Command.Unknown);
        assertTrue(parser.parse("e2e4e5") instanceof Command.Unknown);
        assertTrue(((Command.Unknown) parser.parse("hola")).reason().contains("ayuda"));
    }
}
