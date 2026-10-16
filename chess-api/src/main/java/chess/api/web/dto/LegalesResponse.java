package chess.api.web.dto;

import chess.api.application.model.MovimientoLegal;

import java.util.List;

public record LegalesResponse(String turno, List<MovimientoLegal> jugadas) {
}
