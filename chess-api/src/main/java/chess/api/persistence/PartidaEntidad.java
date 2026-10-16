package chess.api.persistence;

import chess.api.application.model.DatosPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
public class PartidaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jugadorBlanco;

    private String jugadorNegro;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_final")
    private LocalDateTime fechaFin;

    @Column(name = "fen", nullable = false, length = 1024)
    private String fen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private DatosPartida.Estado estado;

    @Column(name = "resultado", length = 512)
    private String resultado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJugadorBlanco() {
        return jugadorBlanco;
    }

    public void setJugadorBlanco(String jugadorBlanco) {
        this.jugadorBlanco = jugadorBlanco;
    }

    public String getJugadorNegro() {
        return jugadorNegro;
    }

    public void setJugadorNegro(String jugadorNegro) {
        this.jugadorNegro = jugadorNegro;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public DatosPartida.Estado getEstado() {
        return estado;
    }

    public void setEstado(DatosPartida.Estado estado) {
        this.estado = estado;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
