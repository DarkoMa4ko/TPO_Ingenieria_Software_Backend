package chess.core.model;

import chess.core.board.Square;

import java.util.Optional;

/**
 * Un movimiento, modelado con el patrón Command: sabe cómo aplicarse sobre un tablero en
 * construcción ({@link Position.Builder}).
 *
 * <p>Cada tipo de movimiento especial (enroque, captura al paso, promoción, avance doble) es
 * una clase propia en el paquete {@code chess.core.move}. {@link Position} y {@code Game}
 * solo conocen esta interfaz, por lo que agregar un movimiento nuevo no requiere modificarlos.
 *
 * <p>Las implementaciones deben tener igualdad por valor (lo natural es usar {@code record}):
 * {@code Game} compara movimientos con {@code equals} para validar los intentos del usuario.
 */
public interface Move {

    /** Casilla de origen de la pieza que se mueve. */
    Square from();

    /** Casilla de destino de la pieza que se mueve. */
    Square to();

    /** Aplica los cambios del movimiento sobre el tablero en construcción. */
    void execute(Position.Builder board);

    /** Pieza en la que se promociona, si el movimiento es una promoción. */
    default Optional<PieceType> promotion() {
        return Optional.empty();
    }

    /** Notación de coordenadas: "e2e4", "e7e8q". */
    default String notation() {
        String suffix = promotion()
                .map(type -> String.valueOf(Character.toLowerCase(type.symbol())))
                .orElse("");
        return from().toString() + to() + suffix;
    }
}
