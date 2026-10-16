package chess.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaCrudRepository extends JpaRepository<PartidaEntidad, Long> {
}
