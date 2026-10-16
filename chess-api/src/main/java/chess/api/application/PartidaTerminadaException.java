package chess.api.application;

public class PartidaTerminadaException extends RuntimeException {

    public PartidaTerminadaException(String resultado) {
        super("La partida ya terminó: " + resultado);
    }
}
