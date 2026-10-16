package chess.api.application;

public class PartidaNoEncontradaException extends RuntimeException {

    public PartidaNoEncontradaException(long id) {
        super("No existe una partida con id " + id);
    }
}
