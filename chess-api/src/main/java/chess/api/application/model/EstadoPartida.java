package chess.api.application.model;

public record EstadoPartida(Long id, String fen, String turno, boolean enJaque, boolean finalizada, String resultado) {
}
