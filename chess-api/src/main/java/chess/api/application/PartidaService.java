package chess.api.application;

import chess.api.application.model.DatosJugada;
import chess.api.application.model.DatosPartida;
import chess.api.application.model.EstadoPartida;
import chess.api.application.model.MovimientoLegal;
import chess.core.board.Square;
import chess.core.game.Game;
import chess.core.game.IllegalMoveException;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.PieceType;
import chess.core.standard.StandardChess;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PartidaService {

    private final PartidaRepository partidas;

    public PartidaService(PartidaRepository partidas) {
        this.partidas = partidas;
    }

    public EstadoPartida crear(String jugadorBlanco, String jugadorNegro, String fen) {
        String fenInicial = fen == null || fen.isBlank() ? StandardChess.START_FEN : fen;
        Game game = StandardChess.newGame(fenInicial);
        DatosPartida creada = partidas.guardar(new DatosPartida(
                null, jugadorBlanco, jugadorNegro,
                LocalDateTime.now(), null,
                StandardChess.fen().format(game.position()),
                DatosPartida.Estado.EN_CURSO, null));
        return estadoDe(creada, game);
    }

    public EstadoPartida jugar(long id, String desde, String hasta, Character promocion) {
        DatosPartida datos = obtener(id);
        if (datos.estado() == DatosPartida.Estado.FINALIZADA) {
            throw new PartidaTerminadaException(datos.resultado() == null ? "Partida finalizada" : datos.resultado());
        }
        Game game = StandardChess.newGame(datos.fen());
        Square origen = Square.parse(desde);
        Square destino = Square.parse(hasta);
        Move movido = promocion == null ? game.play(origen, destino) : game.play(origen, destino, promocion);
        Character piezaPromocion = movido.promotion()
                .map(type -> Character.toLowerCase(type.symbol())).orElse(null);
        String fenNuevo = StandardChess.fen().format(game.position());
        DatosPartida actualizada = cerrarSiTermino(datos, game, fenNuevo);
        partidas.guardar(actualizada);
        partidas.guardarJugada(id, new DatosJugada(
                partidas.cantidadJugadas(id) + 1, movido.from().toString(), movido.to().toString(), piezaPromocion, fenNuevo));
        return estadoDe(actualizada, game);
    }

    public EstadoPartida estado(long id) {
        DatosPartida datos = obtener(id);
        Game game = StandardChess.newGame(datos.fen());
        return estadoDe(datos, game);
    }

    public List<DatosJugada> jugadas(long id) {
        obtener(id);
        return partidas.jugadas(id);
    }

    public List<MovimientoLegal> jugadasLegales(long id, String casilla) {
        DatosPartida datos = obtener(id);
        Game game = StandardChess.newGame(datos.fen());
        Square origen = casilla == null || casilla.isBlank() ? null : Square.parse(casilla);
        List<Move> legales = origen == null ? game.legalMoves() : game.legalMovesFrom(origen);
        return legales.stream()
                .map(m -> new MovimientoLegal(m.from().toString(), m.to().toString(), m.notation()))
                .sorted(Comparator.comparing(MovimientoLegal::notacion))
                .toList();
    }

    public EstadoPartida finalizar(long id, String motivo) {
        DatosPartida datos = obtener(id);
        if (datos.estado() == DatosPartida.Estado.FINALIZADA) {
            throw new PartidaTerminadaException(datos.resultado() == null ? "Partida finalizada" : datos.resultado());
        }
        String texto = motivo == null || motivo.isBlank() ? "Partida abandonada" : motivo;
        DatosPartida cerrada = new DatosPartida(datos.id(), datos.jugadorBlanco(), datos.jugadorNegro(),
                datos.fechaInicio(), LocalDateTime.now(), datos.fen(),
                DatosPartida.Estado.FINALIZADA, texto);
        partidas.guardar(cerrada);
        Game game = StandardChess.newGame(datos.fen());
        return new EstadoPartida(cerrada.id(), datos.fen(), turnoDe(game), game.isInCheck(), true, texto);
    }

    private DatosPartida cerrarSiTermino(DatosPartida datos, Game game, String fenNuevo) {
        if (!game.isOver()) {
            return new DatosPartida(datos.id(), datos.jugadorBlanco(), datos.jugadorNegro(),
                    datos.fechaInicio(), datos.fechaFin(), fenNuevo, DatosPartida.Estado.EN_CURSO, null);
        }
        return new DatosPartida(datos.id(), datos.jugadorBlanco(), datos.jugadorNegro(),
                datos.fechaInicio(), LocalDateTime.now(), fenNuevo,
                DatosPartida.Estado.FINALIZADA, Optional.ofNullable(resultadoDe(game)).orElse("Partida finalizada"));
    }

    private EstadoPartida estadoDe(DatosPartida datos, Game game) {
        String resultado = datos.estado() == DatosPartida.Estado.FINALIZADA ? datos.resultado() : resultadoDe(game);
        boolean gameOver = game.isOver();
        boolean finalizada = datos.estado() == DatosPartida.Estado.FINALIZADA || gameOver;
        return new EstadoPartida(datos.id(), datos.fen(), turnoDe(game), game.isInCheck(), finalizada, resultado);
    }

    private String resultadoDe(Game game) {
        return game.result().map(r -> switch (r.outcome()) {
            case WHITE_WINS -> "Ganan blancas: " + r.reason();
            case BLACK_WINS -> "Ganan negras: " + r.reason();
            case DRAW -> "Tablas: " + r.reason();
        }).orElse(null);
    }

    private String turnoDe(Game game) {
        return game.turn() == Color.WHITE ? "w" : "b";
    }

    private DatosPartida obtener(long id) {
        return partidas.porId(id).orElseThrow(() -> new PartidaNoEncontradaException(id));
    }
}
