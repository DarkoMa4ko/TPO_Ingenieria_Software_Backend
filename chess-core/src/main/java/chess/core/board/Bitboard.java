package chess.core.board;

import java.math.BigInteger;
import java.util.function.IntConsumer;

/**
 * Conjunto inmutable de casillas representado como una máscara de bits: el bit {@code i}
 * está encendido si la casilla de índice {@code i} pertenece al conjunto.
 *
 * <p>Es la estructura central del motor. Las operaciones de conjuntos (unión, intersección,
 * diferencia) y los desplazamientos (shifts) reemplazan a los recorridos casilla por casilla.
 *
 * <p><b>Decisión de diseño:</b> el almacenamiento interno es un {@link BigInteger} para no
 * limitar el tablero a 64 casillas (un {@code long} no alcanza para, por ejemplo, 10x8 = 80).
 * Ninguna otra clase conoce ese detalle: la API pública solo habla de índices de bit. Si se
 * quisiera la versión de máxima velocidad para 8x8, alcanza con reemplazar el campo interno
 * por un {@code long} en esta única clase.
 */
public final class Bitboard {

    public static final Bitboard EMPTY = new Bitboard(BigInteger.ZERO);

    private final BigInteger bits;

    private Bitboard(BigInteger bits) {
        this.bits = bits;
    }

    /** Conjunto con un único elemento: el bit {@code index}. */
    public static Bitboard ofBit(int index) {
        requireNonNegative(index);
        return new Bitboard(BigInteger.ONE.shiftLeft(index));
    }

    public static Bitboard ofBits(int... indices) {
        Bitboard result = EMPTY;
        for (int index : indices) {
            result = result.with(index);
        }
        return result;
    }

    /** Conjunto con los bits {@code 0 .. count-1} encendidos. */
    public static Bitboard filled(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        return new Bitboard(BigInteger.ONE.shiftLeft(count).subtract(BigInteger.ONE));
    }

    // ---- álgebra de conjuntos ------------------------------------------------------------

    public Bitboard and(Bitboard other) {
        return new Bitboard(bits.and(other.bits));
    }

    public Bitboard or(Bitboard other) {
        return new Bitboard(bits.or(other.bits));
    }

    public Bitboard xor(Bitboard other) {
        return new Bitboard(bits.xor(other.bits));
    }

    /** Diferencia de conjuntos: los elementos de este que NO están en {@code other}. */
    public Bitboard andNot(Bitboard other) {
        return new Bitboard(bits.andNot(other.bits));
    }

    public Bitboard with(int index) {
        requireNonNegative(index);
        return new Bitboard(bits.setBit(index));
    }

    public Bitboard without(int index) {
        requireNonNegative(index);
        return new Bitboard(bits.clearBit(index));
    }

    // ---- desplazamientos -----------------------------------------------------------------

    /** Desplaza todos los bits hacia índices mayores. No recorta: de eso se ocupa la geometría. */
    public Bitboard shiftLeft(int positions) {
        return positions == 0 ? this : new Bitboard(bits.shiftLeft(positions));
    }

    /** Desplaza todos los bits hacia índices menores; los que salen por abajo se pierden. */
    public Bitboard shiftRight(int positions) {
        return positions == 0 ? this : new Bitboard(bits.shiftRight(positions));
    }

    // ---- consultas -----------------------------------------------------------------------

    public boolean isEmpty() {
        return bits.signum() == 0;
    }

    public boolean has(int index) {
        return index >= 0 && bits.testBit(index);
    }

    /** {@code true} si comparte al menos una casilla con {@code other}. */
    public boolean intersects(Bitboard other) {
        return bits.and(other.bits).signum() != 0;
    }

    public int cardinality() {
        return bits.bitCount();
    }

    /** Índice del bit encendido más bajo, o -1 si está vacío. */
    public int lowestBit() {
        return bits.getLowestSetBit();
    }

    /** Índice del bit encendido más alto, o -1 si está vacío. */
    public int highestBit() {
        return bits.bitLength() - 1;
    }

    /** Recorre los bits encendidos de menor a mayor. */
    public void forEachBit(IntConsumer action) {
        BigInteger rest = bits;
        while (rest.signum() != 0) {
            int index = rest.getLowestSetBit();
            action.accept(index);
            rest = rest.clearBit(index);
        }
    }

    public int[] toIndices() {
        int[] result = new int[cardinality()];
        int[] next = {0};
        forEachBit(i -> result[next[0]++] = i);
        return result;
    }

    /** Dibuja el conjunto como una grilla de 0/1 (la fila de mayor índice arriba), para depurar. */
    public String toGrid(int files, int ranks) {
        StringBuilder sb = new StringBuilder();
        for (int rank = ranks - 1; rank >= 0; rank--) {
            for (int file = 0; file < files; file++) {
                sb.append(has(rank * files + file) ? '1' : '.');
                if (file < files - 1) {
                    sb.append(' ');
                }
            }
            if (rank > 0) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bitboard other && bits.equals(other.bits);
    }

    @Override
    public int hashCode() {
        return bits.hashCode();
    }

    @Override
    public String toString() {
        return "Bitboard[0x" + bits.toString(16) + "]";
    }

    private static void requireNonNegative(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Índice de bit negativo: " + index);
        }
    }
}
