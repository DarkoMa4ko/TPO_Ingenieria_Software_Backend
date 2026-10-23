package chess.core.standard;

import chess.core.game.Checkmate;
import chess.core.game.EndCondition;
import chess.core.game.Game;
import chess.core.game.LegalMoveGenerator;
import chess.core.game.InsufficientMaterial;
import chess.core.game.Stalemate;
import chess.core.game.ThreefoldRepetition;
import chess.core.model.Position;
import chess.core.game.FiftyMoveRule;

import java.util.List;

/**
 * Raíz de composición del ajedrez clásico: arma las piezas, la posición inicial y las
 * condiciones de fin de partida, y las inyecta en un {@link Game}.
 *
 * <p>Las reglas de finalización del ajedrez estándar se configuran en este punto.
 * Actualmente se contemplan jaque mate, rey ahogado, material insuficiente,
 * tablas por triple repetición y la regla de los cincuenta movimientos.
 * {@code Game} no necesita conocer las implementaciones concretas: únicamente recibe
 * una lista de {@link EndCondition} y las evalúa durante la partida.
 *
 * <p>Una variante (otro tablero, otras piezas u otras reglas de finalización) puede
 * definir otra raíz de composición con la misma forma que esta, sin necesidad de
 * modificar {@code Game} ni las reglas existentes.
 */
public final class StandardChess {

    public static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final Fen FEN = Fen.standard();

    private StandardChess() {
    }

    public static Fen fen() {
        return FEN;
    }

    /**
     * Devuelve la posición inicial del ajedrez estándar.
     */
    public static Position initialPosition() {
        return FEN.parse(START_FEN);
    }

    /**
     * Condiciones de finalización utilizadas por una partida de ajedrez estándar.
     *
     * <p>Cada condición implementa {@link EndCondition}, por lo que {@link Game}
     * puede evaluarlas sin depender de sus implementaciones concretas.
     */
    public static List<EndCondition> endConditions() {
        return List.of(
                new Checkmate(),
                new Stalemate(),
                new InsufficientMaterial(),
                new ThreefoldRepetition(),
                new FiftyMoveRule()
        );
    }

    /**
     * Crea una partida nueva desde la posición inicial del ajedrez estándar.
     */
    public static Game newGame() {
        return newGame(initialPosition());
    }

    /**
     * Crea una partida que arranca desde una posición dada en FEN.
     *
     * <p>Es útil para estudios, tests y escenarios que no necesitan comenzar
     * desde la posición inicial.
     */
    public static Game newGame(String fen) {
        return newGame(FEN.parse(fen));
    }

    /**
     * Crea una partida estándar a partir de una posición ya construida.
     *
     * <p>Este método compone el generador de movimientos y las condiciones
     * de finalización que utilizará {@link Game}.
     */
    public static Game newGame(Position position) {
        return new Game(
                position,
                new LegalMoveGenerator(),
                endConditions()
        );
    }

    /**
     * Reconstruye una partida estándar conservando su historial de posiciones.
     *
     * <p>Este método permite restaurar una partida persistida sin perder
     * el contexto necesario para reglas que dependen del historial,
     * como las tablas por triple repetición.
     *
     * @param current posición actual de la partida
     * @param positionHistory historial de posiciones, incluyendo la inicial
     *                        y la posición actual
     * @return partida estándar reconstruida con su historial
     */
    public static Game restoreGame(
            Position current,
            List<Position> positionHistory
    ) {
        return new Game(
                current,
                new LegalMoveGenerator(),
                endConditions(),
                positionHistory
        );
    }
}

