package chess.api.web.dto;

public record EstadoResponse(long id, String fen, String turno, boolean enJaque, boolean finalizada, String resultado) {
}
