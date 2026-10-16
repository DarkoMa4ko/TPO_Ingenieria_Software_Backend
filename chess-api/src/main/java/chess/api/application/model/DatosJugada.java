package chess.api.application.model;

public record DatosJugada(int numero, String desde, String hasta, Character promocion, String fenResultante) {

    public String notacion() {
        return desde + hasta + (promocion == null ? "" : promocion);
    }
}
