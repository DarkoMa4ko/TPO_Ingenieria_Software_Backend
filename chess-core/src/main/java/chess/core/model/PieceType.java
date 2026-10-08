package chess.core.model;

import java.util.Objects;

/**
 * Tipo de pieza (peón, torre, ...) descrito como <b>datos</b>: nombre, símbolo, regla de
 * movimiento y si es "real" (el rey: la pieza cuyo jaque define el fin de la partida).
 *
 * <p>No es un {@code enum} a propósito: un enum es un conjunto cerrado y agregar una pieza
 * obligaría a modificarlo. Con un objeto de valor, una pieza nueva es simplemente otra
 * instancia ({@code new PieceType("Arzobispo", 'A', ...)}), sin tocar código existente.
 * La conducta se delega en la {@link MovementRule} (composición en lugar de herencia).
 *
 * @param name     nombre para mostrar
 * @param symbol   letra mayúscula que la identifica (notación FEN / tableros de texto)
 * @param movement estrategia de movimiento
 * @param royal    {@code true} si perder esta pieza (jaque mate) termina la partida
 */
public record PieceType(String name, char symbol, MovementRule movement, boolean royal) {

    public PieceType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(movement, "movement");
        if (!Character.isUpperCase(symbol)) {
            throw new IllegalArgumentException("El símbolo debe ser una letra mayúscula: " + symbol);
        }
    }

    /** Pieza común (no real). */
    public PieceType(String name, char symbol, MovementRule movement) {
        this(name, symbol, movement, false);
    }

    @Override
    public String toString() {
        return name;
    }
}
