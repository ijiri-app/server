package ijiri.ijiriserver.domain.part.adapter.out.persistence;

import ijiri.ijiriserver.domain.part.entity.PartAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartAliasJpaRepository extends JpaRepository<PartAlias, Long> {
}
