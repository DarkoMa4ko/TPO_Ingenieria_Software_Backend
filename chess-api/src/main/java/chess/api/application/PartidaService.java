package chess.api.application;

import chess.api.application.model.DatosJugada;
import chess.api.application.model.DatosPartida;
import chess.api.application.model.EstadoPartida;
import chess.api.application.model.MovimientoLegal;
import chess.core.board.Square;
import chess.core.game.Game;
import chess.core.model.Color;
import chess.core.model.Move;
import chess.core.model.Position;
import chess.core.standard.StandardChess;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PartidaService {

    private final PartidaRepository partidas;

    public PartidaService(PartidaRepository partidas) {
        this.partidas = partidas;
    }

    /**
     * Crea una nueva partida.
     *
     * <p>Si no se proporciona un FEN, se utiliza la posición inicial
     * del ajedrez estándar. El FEN inicial se conserva durante toda
     * la vida de la partida para poder reconstruir posteriormente
     * su historial completo de posiciones.
     */
    public EstadoPartida crear(
            String jugadorBlanco,
            String jugadorNegro,
            String fen
    ) {
        String fenSolicitado =
                fen == null || fen.isBlank()
                        ? StandardChess.START_FEN
                        : fen;

        Game game =
                StandardChess.newGame(fenSolicitado);

        String fenInicial =
                StandardChess.fen()
                        .format(game.position());

        DatosPartida creada = partidas.guardar(
                new DatosPartida(
                        null,
                        jugadorBlanco,
                        jugadorNegro,
                        LocalDateTime.now(),
                        null,
                        fenInicial,
                        fenInicial,
                        DatosPartida.Estado.EN_CURSO,
                        null
                )
        );

        return estadoDe(creada, game);
    }

    /**
     * Ejecuta una jugada sobre una partida persistida.
     *
     * <p>Antes de aplicar el movimiento se reconstruye el Game con
     * su historial completo de posiciones, de modo que las reglas
     * dependientes del historial puedan evaluarse correctamente.
     */
    public EstadoPartida jugar(
            long id,
            String desde,
            String hasta,
            Character promocion
    ) {
        DatosPartida datos = obtener(id);

        if (datos.estado() == DatosPartida.Estado.FINALIZADA) {
            throw new PartidaTerminadaException(
                    datos.resultado() == null
                            ? "Partida finalizada"
                            : datos.resultado()
            );
        }

        Game game = reconstruirGame(datos);

        Square origen =
                Square.parse(desde);

        Square destino =
                Square.parse(hasta);

        Move movido = promocion == null
                ? game.play(origen, destino)
                : game.play(origen, destino, promocion);

        Character piezaPromocion = movido.promotion()
                .map(type ->
                        Character.toLowerCase(type.symbol())
                )
                .orElse(null);

        String fenNuevo =
                StandardChess.fen()
                        .format(game.position());

        DatosPartida actualizada =
                cerrarSiTermino(
                        datos,
                        game,
                        fenNuevo
                );

        partidas.guardar(actualizada);

        partidas.guardarJugada(
                id,
                new DatosJugada(
                        partidas.cantidadJugadas(id) + 1,
                        movido.from().toString(),
                        movido.to().toString(),
                        piezaPromocion,
                        fenNuevo
                )
        );

        return estadoDe(
                actualizada,
                game
        );
    }

    /**
     * Devuelve el estado actual de una partida.
     */
    public EstadoPartida estado(long id) {
        DatosPartida datos =
                obtener(id);

        Game game =
                reconstruirGame(datos);

        return estadoDe(
                datos,
                game
        );
    }

    /**
     * Devuelve las jugadas persistidas de una partida.
     */
    public List<DatosJugada> jugadas(long id) {
        obtener(id);

        return partidas.jugadas(id);
    }

    /**
     * Devuelve los movimientos legales disponibles para la partida.
     *
     * <p>Si se proporciona una casilla, se devuelven únicamente los
     * movimientos cuyo origen coincide con ella.
     */
    public List<MovimientoLegal> jugadasLegales(
            long id,
            String casilla
    ) {
        DatosPartida datos =
                obtener(id);

        Game game =
                reconstruirGame(datos);

        Square origen =
                casilla == null || casilla.isBlank()
                        ? null
                        : Square.parse(casilla);

        List<Move> legales = origen == null
                ? game.legalMoves()
                : game.legalMovesFrom(origen);

        return legales.stream()
                .map(move ->
                        new MovimientoLegal(
                                move.from().toString(),
                                move.to().toString(),
                                move.notation()
                        )
                )
                .sorted(
                        Comparator.comparing(
                                MovimientoLegal::notacion
                        )
                )
                .toList();
    }

    /**
     * Finaliza manualmente una partida.
     */
    public EstadoPartida finalizar(
            long id,
            String motivo
    ) {
        DatosPartida datos =
                obtener(id);

        if (datos.estado() == DatosPartida.Estado.FINALIZADA) {
            throw new PartidaTerminadaException(
                    datos.resultado() == null
                            ? "Partida finalizada"
                            : datos.resultado()
            );
        }

        String texto =
                motivo == null || motivo.isBlank()
                        ? "Partida abandonada"
                        : motivo;

        DatosPartida cerrada =
                new DatosPartida(
                        datos.id(),
                        datos.jugadorBlanco(),
                        datos.jugadorNegro(),
                        datos.fechaInicio(),
                        LocalDateTime.now(),

                        // La posición inicial nunca cambia.
                        datos.fenInicial(),

                        // La partida finaliza en su posición actual.
                        datos.fen(),

                        DatosPartida.Estado.FINALIZADA,
                        texto
                );

        partidas.guardar(cerrada);

        Game game =
                reconstruirGame(datos);

        return new EstadoPartida(
                cerrada.id(),
                datos.fen(),
                turnoDe(game),
                game.isInCheck(),
                true,
                texto
        );
    }

    /**
     * Construye los datos persistibles después de una jugada.
     *
     * <p>El FEN inicial se conserva sin modificaciones. Solo cambia
     * el FEN actual y, si corresponde, el estado final de la partida.
     */
    private DatosPartida cerrarSiTermino(
            DatosPartida datos,
            Game game,
            String fenNuevo
    ) {
        if (!game.isOver()) {
            return new DatosPartida(
                    datos.id(),
                    datos.jugadorBlanco(),
                    datos.jugadorNegro(),
                    datos.fechaInicio(),
                    datos.fechaFin(),

                    // P0 permanece inmutable.
                    datos.fenInicial(),

                    // Nueva posición actual.
                    fenNuevo,

                    DatosPartida.Estado.EN_CURSO,
                    null
            );
        }

        return new DatosPartida(
                datos.id(),
                datos.jugadorBlanco(),
                datos.jugadorNegro(),
                datos.fechaInicio(),
                LocalDateTime.now(),

                // P0 permanece inmutable.
                datos.fenInicial(),

                // Posición en la que terminó la partida.
                fenNuevo,

                DatosPartida.Estado.FINALIZADA,
                Optional.ofNullable(
                        resultadoDe(game)
                ).orElse("Partida finalizada")
        );
    }

    /**
     * Reconstruye el Game de dominio a partir de la información persistida.
     *
     * <p>El historial comienza con el FEN inicial de la partida. Después
     * se agregan, en orden, los FEN resultantes de todas las jugadas
     * almacenadas.
     *
     * <p>La capa de aplicación se encarga únicamente de reconstruir
     * los objetos del dominio. Las reglas que utilizan ese historial
     * continúan perteneciendo a chess-core.
     */
    private Game reconstruirGame(
            DatosPartida datos
    ) {
        List<Position> historial =
                new ArrayList<>();

        // P0: posición desde la que comenzó la partida.
        historial.add(
                StandardChess.fen()
                        .parse(datos.fenInicial())
        );

        // P1 ... Pn: posiciones resultantes de cada jugada.
        for (DatosJugada jugada :
                partidas.jugadas(datos.id())) {

            historial.add(
                    StandardChess.fen()
                            .parse(
                                    jugada.fenResultante()
                            )
            );
        }

        // Posición actual persistida en la partida.
        Position actual =
                StandardChess.fen()
                        .parse(datos.fen());

        return StandardChess.restoreGame(
                actual,
                historial
        );
    }

    /**
     * Convierte el estado interno del dominio en la respuesta utilizada
     * por la capa web.
     */
    private EstadoPartida estadoDe(
            DatosPartida datos,
            Game game
    ) {
        String resultado =
                datos.estado() == DatosPartida.Estado.FINALIZADA
                        ? datos.resultado()
                        : resultadoDe(game);

        boolean gameOver =
                game.isOver();

        boolean finalizada =
                datos.estado() == DatosPartida.Estado.FINALIZADA
                        || gameOver;

        return new EstadoPartida(
                datos.id(),
                datos.fen(),
                turnoDe(game),
                game.isInCheck(),
                finalizada,
                resultado
        );
    }

    /**
     * Traduce el GameResult del núcleo a la representación textual
     * utilizada por la API.
     */
    private String resultadoDe(Game game) {
        return game.result()
                .map(result -> switch (result.outcome()) {
                    case WHITE_WINS ->
                            "Ganan blancas: "
                                    + result.reason();

                    case BLACK_WINS ->
                            "Ganan negras: "
                                    + result.reason();

                    case DRAW ->
                            "Tablas: "
                                    + result.reason();
                })
                .orElse(null);
    }

    /**
     * Convierte el color al que le corresponde mover a la
     * representación utilizada por la API.
     */
    private String turnoDe(Game game) {
        return game.turn() == Color.WHITE
                ? "w"
                : "b";
    }

    /**
     * Recupera una partida o lanza la excepción correspondiente
     * cuando el identificador no existe.
     */
    private DatosPartida obtener(long id) {
        return partidas.porId(id)
                .orElseThrow(
                        () ->
                                new PartidaNoEncontradaException(id)
                );
    }
}