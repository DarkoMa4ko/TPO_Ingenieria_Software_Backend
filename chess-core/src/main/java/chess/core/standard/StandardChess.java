package chess.core.standard;

import chess.core.game.Checkmate;
import chess.core.game.EndCondition;
import chess.core.game.Game;
import chess.core.game.LegalMoveGenerator;
import chess.core.game.Stalemate;
import chess.core.model.Position;

import java.util.List;

/**
 * Raíz de composición del ajedrez clásico: arma las piezas, la posición inicial y las
 * condiciones de fin de partida, y las inyecta en un {@link Game}.
 *
 * <p>Una variante (otro tablero, otras piezas, otras reglas de final) es otra clase con la
 * misma forma que esta; no hace falta modificar {@code Game} ni ninguna regla.
 */
public final class StandardChess {

    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final Fen FEN = Fen.standard();

    private StandardChess() {
    }

    public static Fen fen() {
        return FEN;
    }

    public static Position initialPosition() {
        return FEN.parse(START_FEN);
    }

    public static List<EndCondition> endConditions() {
        return List.of(new Checkmate(), new Stalemate());
    }

    /** Partida nueva desde la posición inicial. */
    public static Game newGame() {
        return newGame(initialPosition());
    }

    /** Partida que arranca desde una posición dada en FEN (útil para estudios y tests). */
    public static Game newGame(String fen) {
        return newGame(FEN.parse(fen));
    }

    public static Game newGame(Position position) {
        return new Game(position, new LegalMoveGenerator(), endConditions());
    }
}
