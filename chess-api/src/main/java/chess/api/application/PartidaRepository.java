package chess.api.application;

import chess.api.application.model.DatosJugada;
import chess.api.application.model.DatosPartida;

import java.util.List;
import java.util.Optional;

public interface PartidaRepository {

    DatosPartida guardar(DatosPartida partida);

    Optional<DatosPartida> porId(long id);

    void guardarJugada(long partidaId, DatosJugada jugada);

    List<DatosJugada> jugadas(long partidaId);

    int cantidadJugadas(long partidaId);
}
