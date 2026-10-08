package chess.core;

import chess.core.board.Square;
import chess.core.game.Game;
import chess.core.game.LegalMoveGenerator;
import chess.core.model.Move;
import chess.core.model.Position;
import chess.core.standard.Fen;

import java.util.List;
import java.util.TreeSet;

/** Utilidades compartidas por los tests: construir posiciones desde FEN y consultar movimientos. */
public final class TestSupport {

    private static final Fen FEN = Fen.standard();
    private static final LegalMoveGenerator GENERATOR = new LegalMoveGenerator();

    private TestSupport() {
    }

    public static Position position(String fen) {
        return FEN.parse(fen);
    }

    public static Square sq(String text) {
        return Square.parse(text);
    }

    /** Notaciones de todos los movimientos legales de la posición, ordenadas (p. ej. "e2e4"). */
    public static List<String> legalMoves(String fen) {
        return notations(GENERATOR.legalMoves(position(fen)));
    }

    /** Notaciones de los movimientos legales de la pieza ubicada en {@code from}. */
    public static List<String> legalMovesFrom(String fen, String from) {
        Square origin = sq(from);
        List<Move> moves = GENERATOR.legalMoves(position(fen)).stream()
                .filter(m -> m.from().equals(origin)).toList();
        return notations(moves);
    }

    /** Casillas de destino (sin repetir, ordenadas) de los movimientos legales desde {@code from}. */
    public static List<String> targets(String fen, String from) {
        Square origin = sq(from);
        TreeSet<String> result = new TreeSet<>();
        for (Move m : GENERATOR.legalMoves(position(fen))) {
            if (m.from().equals(origin)) {
                result.add(m.to().toString());
            }
        }
        return List.copyOf(result);
    }

    public static List<String> notations(List<Move> moves) {
        return moves.stream().map(Move::notation).sorted().toList();
    }

    /** Notaciones (ordenadas) de los movimientos legales de {@code game} desde una casilla. */
    public static List<String> notationsFrom(Game game, String from) {
        return notations(game.legalMovesFrom(sq(from)));
    }

    /** Juega una jugada en notación de coordenadas ("e2e4", "e7e8q") sobre la partida. */
    public static Move play(Game game, String notation) {
        Square from = sq(notation.substring(0, 2));
        Square to = sq(notation.substring(2, 4));
        return notation.length() == 5
                ? game.play(from, to, notation.charAt(4))
                : game.play(from, to);
    }

    /** Juega varias jugadas separadas por espacios. */
    public static void playAll(Game game, String moves) {
        for (String move : moves.trim().split("\\s+")) {
            play(game, move);
        }
    }
}
