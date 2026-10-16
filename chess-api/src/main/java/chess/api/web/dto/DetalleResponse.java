package chess.api.web.dto;

import java.util.List;

public record DetalleResponse(EstadoResponse estado, List<JugadaResponse> jugadas) {
}
