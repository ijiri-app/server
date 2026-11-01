package ijiri.ijiriserver.domain.part.application.port.out;

import ijiri.ijiriserver.domain.part.entity.Brand;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BrandPort {

    Optional<Brand> findByNormalizedName(String normalizedName);

    List<Brand> findByNormalizedNameContainingOrderByNameAsc(String normalizedKeyword, Limit limit);

    void insertIfAbsent(String name, String normalizedName, LocalDateTime now);

    List<Brand> findAllById(Iterable<Long> ids);

    Optional<Brand> findById(Long id);
}
