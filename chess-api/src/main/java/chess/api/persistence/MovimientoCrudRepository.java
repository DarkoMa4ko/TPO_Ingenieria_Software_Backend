package chess.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCrudRepository extends JpaRepository<MovimientoEntidad, Long> {

    List<MovimientoEntidad> findByPartidaIdOrderByNumeroAsc(Long partidaId);

    long countByPartidaId(Long partidaId);
}
