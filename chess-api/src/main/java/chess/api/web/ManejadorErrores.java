package chess.api.web;

import chess.api.application.PartidaNoEncontradaException;
import chess.api.application.PartidaTerminadaException;
import chess.core.game.IllegalMoveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(PartidaNoEncontradaException.class)
    public ResponseEntity<Error> partidaNoEncontrada(PartidaNoEncontradaException e) {
        return responder(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PartidaTerminadaException.class)
    public ResponseEntity<Error> partidaTerminada(PartidaTerminadaException e) {
        return responder(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({IllegalMoveException.class, IllegalArgumentException.class})
    public ResponseEntity<Error> solicitudInvalida(RuntimeException e) {
        return responder(e, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Error> responder(RuntimeException e, HttpStatus estado) {
        return ResponseEntity.status(estado).body(new Error(estado.value(), e.getMessage()));
    }

    public record Error(int estado, String mensaje) {
    }
}
