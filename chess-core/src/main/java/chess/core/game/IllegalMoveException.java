package chess.core.game;

/** Se intentó un movimiento que las reglas no permiten. El mensaje explica el motivo. */
public class IllegalMoveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IllegalMoveException(String message) {
        super(message);
    }
}
