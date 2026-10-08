package chess.core.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Un paso sobre el tablero: cuántas columnas y cuántas filas se avanza.
 * Sirve tanto para piezas que "deslizan" (torre, alfil, dama) como para las que "saltan"
 * (caballo, rey): la diferencia está en cómo lo usa la regla de movimiento.
 */
public record Direction(int fileStep, int rankStep) {

    public Direction {
        if (fileStep == 0 && rankStep == 0) {
            throw new IllegalArgumentException("Una dirección no puede ser (0, 0)");
        }
    }

    public static final Direction NORTH = new Direction(0, 1);
    public static final Direction SOUTH = new Direction(0, -1);
    public static final Direction EAST = new Direction(1, 0);
    public static final Direction WEST = new Direction(-1, 0);
    public static final Direction NORTH_EAST = new Direction(1, 1);
    public static final Direction NORTH_WEST = new Direction(-1, 1);
    public static final Direction SOUTH_EAST = new Direction(1, -1);
    public static final Direction SOUTH_WEST = new Direction(-1, -1);

    public static final List<Direction> ORTHOGONAL = List.of(NORTH, SOUTH, EAST, WEST);
    public static final List<Direction> DIAGONAL = List.of(NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST);
    public static final List<Direction> ALL = concat(ORTHOGONAL, DIAGONAL);
    /** Los 8 saltos en "L" del caballo. */
    public static final List<Direction> KNIGHT_JUMPS = symmetries(1, 2);

    public Direction opposite() {
        return new Direction(-fileStep, -rankStep);
    }

    /**
     * Las variantes por simetría de un salto (a, b): (±a, ±b) y (±b, ±a), sin repetidos.
     * Ejemplos: {@code symmetries(1, 2)} son los 8 saltos del caballo;
     * {@code symmetries(1, 3)} serían los del "camello" de variantes como el ajedrez de Tamerlán.
     */
    public static List<Direction> symmetries(int a, int b) {
        Set<Direction> result = new LinkedHashSet<>();
        int[] signs = {1, -1};
        for (int sa : signs) {
            for (int sb : signs) {
                if (a != 0 || b != 0) {
                    result.add(new Direction(sa * a, sb * b));
                    result.add(new Direction(sa * b, sb * a));
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    private static List<Direction> concat(List<Direction> first, List<Direction> second) {
        List<Direction> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
