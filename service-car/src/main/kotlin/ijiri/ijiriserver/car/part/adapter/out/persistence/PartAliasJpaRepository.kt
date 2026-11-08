package ijiri.ijiriserver.car.part.adapter.out.persistence

import ijiri.ijiriserver.car.part.entity.PartAlias
import org.springframework.data.jpa.repository.JpaRepository

interface PartAliasJpaRepository : JpaRepository<PartAlias, Long>
