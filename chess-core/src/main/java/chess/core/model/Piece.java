package chess.core.model;

import java.util.Objects;

/** Una pieza concreta sobre el tablero: un {@link PieceType} de un {@link Color}. */
public record Piece(PieceType type, Color color) {

    public Piece {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(color, "color");
    }

    /** Letra de la pieza: mayúscula para las blancas, minúscula para las negras. */
    public char symbol() {
        return color == Color.WHITE ? type.symbol() : Character.toLowerCase(type.symbol());
    }

    @Override
    public String toString() {
        return type.name() + " " + (color == Color.WHITE ? "blanco" : "negro");
    }
}
