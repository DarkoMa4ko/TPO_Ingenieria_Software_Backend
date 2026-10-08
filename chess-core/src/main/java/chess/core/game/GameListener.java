package chess.core.game;

import chess.core.model.Move;

/**
 * Puerto de salida del núcleo (patrón Observer): así el juego avisa lo que ocurre sin conocer
 * a quién le interesa (consola, interfaz gráfica, registro, ...). El núcleo define la interfaz
 * y los adaptadores la implementan, de modo que la dependencia apunta hacia adentro.
 */
public interface GameListener {

    /** Se jugó un movimiento; {@code game} ya refleja la posición resultante. */
    default void onMovePlayed(Game game, Move move) {
    }

    /** La partida terminó. */
    default void onGameOver(Game game, GameResult result) {
    }
}
