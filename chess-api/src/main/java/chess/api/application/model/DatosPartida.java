package chess.api.application.model;

import java.time.LocalDateTime;

/**
 * Datos persistibles de una partida.
 *
 * <p>{@code fenInicial} conserva la posición desde la que comenzó la partida,
 * mientras que {@code fen} representa su posición actual.
 *
 * <p>Mantener ambas posiciones permite reconstruir posteriormente el historial
 * completo de posiciones junto con los FEN resultantes de cada jugada.
 */
public record DatosPartida(
        Long id,
        String jugadorBlanco,
        String jugadorNegro,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String fenInicial,
        String fen,
        Estado estado,
        String resultado) {

    public enum Estado {
        EN_CURSO,
        FINALIZADA
    }
}