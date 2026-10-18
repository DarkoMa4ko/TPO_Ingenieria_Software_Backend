package chess.api.persistence;

import chess.api.application.PartidaRepository;
import chess.api.application.model.DatosJugada;
import chess.api.application.model.DatosPartida;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PartidaRepositoryJpa implements PartidaRepository {

    private final PartidaCrudRepository partidas;
    private final MovimientoCrudRepository movimientos;

    public PartidaRepositoryJpa(
            PartidaCrudRepository partidas,
            MovimientoCrudRepository movimientos
    ) {
        this.partidas = partidas;
        this.movimientos = movimientos;
    }

    @Override
    public DatosPartida guardar(DatosPartida partida) {
        PartidaEntidad entidad = partida.id() == null
                ? new PartidaEntidad()
                : partidas.findById(partida.id())
                .orElse(new PartidaEntidad());

        entidad.setJugadorBlanco(partida.jugadorBlanco());
        entidad.setJugadorNegro(partida.jugadorNegro());
        entidad.setFechaInicio(partida.fechaInicio());
        entidad.setFechaFin(partida.fechaFin());

        // La posición inicial se conserva durante toda la vida de la partida.
        entidad.setFenInicial(partida.fenInicial());

        // El FEN actual cambia después de cada movimiento.
        entidad.setFen(partida.fen());

        entidad.setEstado(partida.estado());
        entidad.setResultado(partida.resultado());

        PartidaEntidad guardada = partidas.save(entidad);

        return new DatosPartida(
                guardada.getId(),
                guardada.getJugadorBlanco(),
                guardada.getJugadorNegro(),
                guardada.getFechaInicio(),
                guardada.getFechaFin(),
                guardada.getFenInicial(),
                guardada.getFen(),
                guardada.getEstado(),
                guardada.getResultado()
        );
    }

    @Override
    public Optional<DatosPartida> porId(long id) {
        return partidas.findById(id)
                .map(this::aDatos);
    }

    @Override
    public void guardarJugada(
            long partidaId,
            DatosJugada jugada
    ) {
        MovimientoEntidad entidad =
                new MovimientoEntidad();

        entidad.setPartida(
                partidas.getReferenceById(partidaId)
        );

        entidad.setNumero(jugada.numero());
        entidad.setDesde(jugada.desde());
        entidad.setHasta(jugada.hasta());
        entidad.setPromocion(jugada.promocion());
        entidad.setFenResultante(jugada.fenResultante());

        movimientos.save(entidad);
    }

    @Override
    public List<DatosJugada> jugadas(long partidaId) {
        return movimientos
                .findByPartidaIdOrderByNumeroAsc(partidaId)
                .stream()
                .map(this::aDatosJugada)
                .toList();
    }

    @Override
    public int cantidadJugadas(long partidaId) {
        return (int) movimientos.countByPartidaId(partidaId);
    }

    /**
     * Convierte la entidad JPA en el modelo utilizado por la capa de aplicación.
     */
    private DatosPartida aDatos(PartidaEntidad entidad) {
        return new DatosPartida(
                entidad.getId(),
                entidad.getJugadorBlanco(),
                entidad.getJugadorNegro(),
                entidad.getFechaInicio(),
                entidad.getFechaFin(),
                entidad.getFenInicial(),
                entidad.getFen(),
                entidad.getEstado(),
                entidad.getResultado()
        );
    }

    /**
     * Convierte un movimiento persistido en los datos de jugada
     * utilizados por la capa de aplicación.
     */
    private DatosJugada aDatosJugada(MovimientoEntidad entidad) {
        return new DatosJugada(
                entidad.getNumero(),
                entidad.getDesde(),
                entidad.getHasta(),
                entidad.getPromocion(),
                entidad.getFenResultante()
        );
    }
}