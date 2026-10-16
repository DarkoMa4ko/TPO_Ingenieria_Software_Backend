package chess.api.application.model;

import java.time.LocalDateTime;

public record DatosPartida(
        Long id,
        String jugadorBlanco,
        String jugadorNegro,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String fen,
        Estado estado,
        String resultado) {

    public enum Estado {
        EN_CURSO,
        FINALIZADA
    }
}
