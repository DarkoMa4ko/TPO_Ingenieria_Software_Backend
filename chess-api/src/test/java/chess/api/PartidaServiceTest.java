package chess.api;

import chess.api.application.PartidaNoEncontradaException;
import chess.api.application.PartidaService;
import chess.api.application.PartidaTerminadaException;
import chess.api.application.model.EstadoPartida;
import chess.core.game.IllegalMoveException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-test;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class PartidaServiceTest {

    @Autowired
    private PartidaService service;

    @Test
    void creaPartidaConPosicionInicial() {
        EstadoPartida creada = service.crear(null, null, null);
        assertTrue(creada.id() > 0);
        assertEquals("w", creada.turno());
        assertFalse(creada.finalizada());
        assertFalse(creada.enJaque());
        assertNull(creada.resultado());
        assertTrue(creada.fen().startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w"));
    }

    @Test
    void juegaYEspecificaHistorial() {
        EstadoPartida creada = service.crear(null, null, null);
        EstadoPartida trasJugada = service.jugar(creada.id(), "e2", "e4", null);
        assertEquals("b", trasJugada.turno());
        assertNotEquals(creada.fen(), trasJugada.fen());
        assertEquals(1, service.jugadas(creada.id()).size());
        EstadoPartida estado = service.estado(creada.id());
        assertEquals(trasJugada.fen(), estado.fen());
    }

    @Test
    void rechazaMovimientoIlegal() {
        EstadoPartida creada = service.crear(null, null, null);
        assertThrows(IllegalMoveException.class, () -> service.jugar(creada.id(), "e2", "e5", null));
    }

    @Test
    void rechazaCasillaInexistente() {
        EstadoPartida creada = service.crear(null, null, null);
        assertThrows(IllegalMoveException.class, () -> service.jugar(creada.id(), "z9", "e5", null));
    }

    @Test
    void detectaJaqueMateYFinaliza() {
        EstadoPartida creada = service.crear(null, null, null);
        long id = creada.id();
        service.jugar(id, "f2", "f3", null);
        service.jugar(id, "e7", "e5", null);
        service.jugar(id, "g2", "g4", null);
        EstadoPartida trasMate = service.jugar(id, "d8", "h4", null);
        assertTrue(trasMate.finalizada());
        assertTrue(trasMate.resultado().toLowerCase().contains("mate"));
        assertThrows(PartidaTerminadaException.class, () -> service.jugar(id, "a2", "a3", null));
    }

    @Test
    void listaJugadasLegales() {
        EstadoPartida creada = service.crear(null, null, null);
        assertFalse(service.jugadasLegales(creada.id(), null).isEmpty());
        assertFalse(service.jugadasLegales(creada.id(), "e2").isEmpty());
        assertTrue(service.jugadasLegales(creada.id(), "e3").isEmpty());
    }

    @Test
    void permitePromocion() {
        EstadoPartida creada = service.crear(null, null,
                "8/p6P/8/8/8/8/k6K/8 w - - 0 1");
        EstadoPartida trasPromocion = service.jugar(creada.id(), "h7", "h8", 'Q');
        assertTrue(trasPromocion.fen().contains("Q"));
        assertEquals(1, service.jugadas(creada.id()).size());
    }

    @Test
    void pidePromocionSiEsAmbigua() {
        EstadoPartida creada = service.crear(null, null, "8/p6P/8/8/8/8/k6K/8 w - - 0 1");
        assertThrows(IllegalMoveException.class, () -> service.jugar(creada.id(), "h7", "h8", null));
    }

    @Test
    void finalizaPorRendicion() {
        EstadoPartida creada = service.crear("Ana", "Beto", null);
        EstadoPartida cerrada = service.finalizar(creada.id(), "Se rinden las blancas");
        assertTrue(cerrada.finalizada());
        assertEquals("Se rinden las blancas", cerrada.resultado());
        assertThrows(PartidaTerminadaException.class, () -> service.finalizar(creada.id(), null));
    }

    @Test
    void fallaSiLaPartidaNoExiste() {
        assertThrows(PartidaNoEncontradaException.class, () -> service.estado(999));
    }
}
