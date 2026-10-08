package chess.core.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forma del tablero (cantidad de columnas y filas) y toda la aritmética que conecta
 * {@link Square} con los índices de {@link Bitboard}.
 *
 * <p>Los índices se asignan fila por fila: {@code index = fila * columnas + columna}, de modo que
 * a1 = 0, b1 = 1, ..., h1 = 7, a2 = 8 (en 8x8). Subir una fila equivale a sumar {@code columnas}
 * al índice, como explica el artículo de bitboards.
 *
 * <p>Esta clase es la que hace que el tamaño del tablero sea un <i>parámetro</i> y no una
 * constante: ninguna regla de movimiento escribe "8", "7" o "9" a mano; todo se deriva de acá.
 */
public final class BoardGeometry {

    private static final Map<Long, BoardGeometry> CACHE = new ConcurrentHashMap<>();

    /** El tablero clásico de 8x8. */
    public static final BoardGeometry STANDARD = of(8, 8);

    private final int files;
    private final int ranks;
    private final Bitboard all;
    private final Bitboard[] fileMasks;
    private final Bitboard[] rankMasks;
    // Cachés perezosos: se calculan una sola vez por forma de tablero.
    private final Map<Integer, Bitboard> sourceMasks = new ConcurrentHashMap<>();
    private final Map<Direction, Bitboard[]> rays = new ConcurrentHashMap<>();

    private BoardGeometry(int files, int ranks) {
        this.files = files;
        this.ranks = ranks;
        this.all = Bitboard.filled(files * ranks);
        this.fileMasks = new Bitboard[files];
        this.rankMasks = new Bitboard[ranks];
        for (int f = 0; f < files; f++) {
            Bitboard mask = Bitboard.EMPTY;
            for (int r = 0; r < ranks; r++) {
                mask = mask.with(r * files + f);
            }
            fileMasks[f] = mask;
        }
        for (int r = 0; r < ranks; r++) {
            Bitboard mask = Bitboard.EMPTY;
            for (int f = 0; f < files; f++) {
                mask = mask.with(r * files + f);
            }
            rankMasks[r] = mask;
        }
    }

    /** Devuelve (y reutiliza) la geometría de {@code files} columnas por {@code ranks} filas. */
    public static BoardGeometry of(int files, int ranks) {
        if (files < 1 || files > 26 || ranks < 1 || ranks > 99) {
            throw new IllegalArgumentException("Tablero fuera de rango: " + files + "x" + ranks);
        }
        return CACHE.computeIfAbsent(((long) files << 32) | ranks, k -> new BoardGeometry(files, ranks));
    }

    public int files() {
        return files;
    }

    public int ranks() {
        return ranks;
    }

    public int squareCount() {
        return files * ranks;
    }

    // ---- casillas <-> índices ------------------------------------------------------------

    public boolean contains(Square square) {
        return square.file() < files && square.rank() < ranks;
    }

    public int index(Square square) {
        if (!contains(square)) {
            throw new IllegalArgumentException("La casilla " + square + " no existe en un tablero " + this);
        }
        return square.rank() * files + square.file();
    }

    public Square square(int index) {
        if (index < 0 || index >= squareCount()) {
            throw new IllegalArgumentException("Índice fuera del tablero: " + index);
        }
        return new Square(index % files, index / files);
    }

    /** Conjunto con una sola casilla. */
    public Bitboard bit(Square square) {
        return Bitboard.ofBit(index(square));
    }

    /** Todas las casillas del tablero. */
    public Bitboard all() {
        return all;
    }

    /** Complemento respecto del tablero (el {@code ~} del artículo, pero acotado al tablero). */
    public Bitboard complement(Bitboard board) {
        return all.andNot(board);
    }

    public Bitboard fileMask(int file) {
        return fileMasks[file];
    }

    public Bitboard rankMask(int rank) {
        return rankMasks[rank];
    }

    /** Las casillas del conjunto, en orden de índice ascendente. */
    public List<Square> squares(Bitboard board) {
        List<Square> result = new ArrayList<>(board.cardinality());
        board.forEachBit(i -> result.add(square(i)));
        return Collections.unmodifiableList(result);
    }

    // ---- desplazamientos con máscaras ----------------------------------------------------

    /**
     * Mueve todas las casillas del conjunto un paso en la dirección dada, descartando las que
     * se salen del tablero.
     *
     * <p>Es la generalización de las líneas del artículo, p. ej.
     * {@code (pawns & ~FILE_A) << 7}: primero se enmascaran las columnas desde las que el paso
     * se saldría por el costado (evita el "wrap-around" a la fila vecina), después se desplaza
     * {@code filaPaso * columnas + columnaPaso} posiciones y, por último, se recorta a las
     * casillas reales del tablero.
     */
    public Bitboard shift(Bitboard board, Direction direction) {
        Bitboard movable = board.and(sourcesFor(direction.fileStep()));
        int delta = direction.rankStep() * files + direction.fileStep();
        Bitboard moved = delta >= 0 ? movable.shiftLeft(delta) : movable.shiftRight(-delta);
        return moved.and(all);
    }

    /** Columnas desde las que se puede aplicar un paso horizontal de {@code fileStep} sin salirse. */
    private Bitboard sourcesFor(int fileStep) {
        return sourceMasks.computeIfAbsent(fileStep, step -> {
            Bitboard mask = Bitboard.EMPTY;
            for (int f = 0; f < files; f++) {
                int target = f + step;
                if (target >= 0 && target < files) {
                    mask = mask.or(fileMasks[f]);
                }
            }
            return mask;
        });
    }

    // ---- rayos (para piezas deslizantes) -------------------------------------------------

    /**
     * Casillas que se alcanzan partiendo de {@code squareIndex} y avanzando en {@code direction}
     * hasta el borde, sin incluir la casilla de partida. Se precalcula una vez por dirección.
     */
    public Bitboard ray(int squareIndex, Direction direction) {
        return rays.computeIfAbsent(direction, this::computeRays)[squareIndex];
    }

    /** {@code true} si avanzar en esa dirección aumenta el índice de bit (norte, este, ...). */
    public boolean isIncreasing(Direction direction) {
        return direction.rankStep() * files + direction.fileStep() > 0;
    }

    private Bitboard[] computeRays(Direction direction) {
        Bitboard[] result = new Bitboard[squareCount()];
        for (int index = 0; index < result.length; index++) {
            Bitboard ray = Bitboard.EMPTY;
            int file = index % files + direction.fileStep();
            int rank = index / files + direction.rankStep();
            while (file >= 0 && file < files && rank >= 0 && rank < ranks) {
                ray = ray.with(rank * files + file);
                file += direction.fileStep();
                rank += direction.rankStep();
            }
            result[index] = ray;
        }
        return result;
    }

    /**
     * Casillas estrictamente entre {@code a} y {@code b} si están alineadas (misma fila,
     * columna o diagonal); vacío en cualquier otro caso. Útil para el enroque.
     */
    public Bitboard between(Square a, Square b) {
        int fileDistance = b.file() - a.file();
        int rankDistance = b.rank() - a.rank();
        boolean aligned = fileDistance == 0 || rankDistance == 0
                || Math.abs(fileDistance) == Math.abs(rankDistance);
        if (!aligned || (fileDistance == 0 && rankDistance == 0)) {
            return Bitboard.EMPTY;
        }
        Direction direction = new Direction(Integer.signum(fileDistance), Integer.signum(rankDistance));
        return ray(index(a), direction).and(ray(index(b), direction.opposite()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BoardGeometry other && files == other.files && ranks == other.ranks;
    }

    @Override
    public int hashCode() {
        return 31 * files + ranks;
    }

    @Override
    public String toString() {
        return files + "x" + ranks;
    }
}
