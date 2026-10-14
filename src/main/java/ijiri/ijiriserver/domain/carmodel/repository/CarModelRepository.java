package ijiri.ijiriserver.domain.carmodel.repository;

import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    // keyword 는 소문자, 빈 문자열이면 전체. coreOnly 가 true 면 핵심 계열만
    @Query("""
            SELECT m FROM CarModel m
            WHERE m.hidden = false
              AND (:coreOnly = false OR m.core = true)
              AND (LOWER(m.name) LIKE CONCAT('%', :keyword, '%') OR LOWER(m.brand) LIKE CONCAT('%', :keyword, '%'))
            ORDER BY m.core DESC, m.displayOrder ASC, m.id ASC
            """)
    List<CarModel> search(@Param("keyword") String keyword, @Param("coreOnly") boolean coreOnly);

    Optional<CarModel> findByIdAndHiddenFalse(Long id);

    Optional<CarModel> findByBrandAndName(String brand, String name);

    long countByIdInAndHiddenFalse(Collection<Long> ids);
}
