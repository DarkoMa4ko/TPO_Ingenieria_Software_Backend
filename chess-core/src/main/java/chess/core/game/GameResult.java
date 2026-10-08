package chess.core.game;

import chess.core.model.Color;

import java.util.Objects;

/**
 * Cómo terminó la partida: el resultado y el motivo (texto para mostrar al usuario).
 */
public record GameResult(Outcome outcome, String reason) {

    public GameResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(reason, "reason");
    }

    public static GameResult win(Color winner, String reason) {
        return new GameResult(winner == Color.WHITE ? Outcome.WHITE_WINS : Outcome.BLACK_WINS, reason);
    }

    public static GameResult draw(String reason) {
        return new GameResult(Outcome.DRAW, reason);
    }
}
