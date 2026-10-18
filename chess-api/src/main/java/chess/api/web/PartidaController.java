
package chess.api.web;

import chess.api.application.PartidaNoEncontradaException;
import chess.api.application.PartidaService;
import chess.api.application.PartidaTerminadaException;
import chess.api.application.model.DatosJugada;
import chess.api.application.model.EstadoPartida;
import chess.api.web.dto.DetalleResponse;
import chess.api.web.dto.EstadoResponse;
import chess.api.web.dto.FinalizarRequest;
import chess.api.web.dto.JugadaResponse;
import chess.api.web.dto.LegalesResponse;
import chess.api.web.dto.MovimientoRequest;
import chess.api.web.dto.NuevaPartidaRequest;
import chess.core.game.IllegalMoveException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    private final PartidaService service;

    public PartidaController(PartidaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoResponse crear(@RequestBody(required = false) NuevaPartidaRequest request) {
        NuevaPartidaRequest dados = request == null ? new NuevaPartidaRequest(null, null, null) : request;
        return aEstado(service.crear(dados.jugadorBlanco(), dados.jugadorNegro(), dados.fen()));
    }

    @PostMapping("/{id}/movimientos")
    public EstadoResponse jugar(@PathVariable long id, @RequestBody MovimientoRequest request) {
        return aEstado(service.jugar(id, request.desde(), request.hasta(), request.promocion()));
    }

    @GetMapping("/{id}")
    public DetalleResponse estado(@PathVariable long id) {
        List<JugadaResponse> jugadas = service.jugadas(id).stream()
                .map(PartidaController::aJugada)
                .toList();
        return new DetalleResponse(aEstado(service.estado(id)), jugadas);
    }

    @GetMapping("/{id}/jugadas")
    public LegalesResponse legales(@PathVariable long id,
            @RequestParam(name = "casilla", required = false) String casilla) {
        EstadoResponse estado = aEstado(service.estado(id));
        return new LegalesResponse(estado.turno(), service.jugadasLegales(id, casilla));
    }

    @PutMapping("/{id}/finalizar")
    public EstadoResponse finalizar(@PathVariable long id, @RequestBody(required = false) FinalizarRequest request) {
        FinalizarRequest datos = request == null ? new FinalizarRequest(null) : request;
        return aEstado(service.finalizar(id, datos.motivo()));
    }

    private static EstadoResponse aEstado(EstadoPartida estado) {
        return new EstadoResponse(estado.id(), estado.fen(), estado.turno(), estado.enJaque(),
                estado.finalizada(), estado.resultado());
    }

    private static JugadaResponse aJugada(DatosJugada jugada) {
        return new JugadaResponse(jugada.numero(), jugada.desde(), jugada.hasta(), jugada.notacion(), jugada.fenResultante());
    }
}
