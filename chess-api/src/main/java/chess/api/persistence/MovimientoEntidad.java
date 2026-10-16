package chess.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "movimiento", uniqueConstraints = @UniqueConstraint(columnNames = {"partida_id", "numero"}))
public class MovimientoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private PartidaEntidad partida;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "desde", nullable = false, length = 4)
    private String desde;

    @Column(name = "hasta", nullable = false, length = 4)
    private String hasta;

    @Column(name = "promocion", length = 1)
    private Character promocion;

    @Column(name = "fen", nullable = false, length = 1024)
    private String fenResultante;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PartidaEntidad getPartida() {
        return partida;
    }

    public void setPartida(PartidaEntidad partida) {
        this.partida = partida;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public Character getPromocion() {
        return promocion;
    }

    public void setPromocion(Character promocion) {
        this.promocion = promocion;
    }

    public String getFenResultante() {
        return fenResultante;
    }

    public void setFenResultante(String fenResultante) {
        this.fenResultante = fenResultante;
    }
}
