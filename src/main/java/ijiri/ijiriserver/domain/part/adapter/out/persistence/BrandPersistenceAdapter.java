package ijiri.ijiriserver.domain.part.adapter.out.persistence;

import ijiri.ijiriserver.domain.part.application.port.out.BrandPort;
import ijiri.ijiriserver.domain.part.entity.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BrandPersistenceAdapter implements BrandPort {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findByNormalizedName(String normalizedName) {
        return brandJpaRepository.findByNormalizedName(normalizedName);
    }

    @Override
    public List<Brand> findByNormalizedNameContainingOrderByNameAsc(String normalizedKeyword, Limit limit) {
        return brandJpaRepository.findByNormalizedNameContainingOrderByNameAsc(normalizedKeyword, limit);
    }

    @Override
    public void insertIfAbsent(String name, String normalizedName, LocalDateTime now) {
        brandJpaRepository.insertIfAbsent(name, normalizedName, now);
    }

    @Override
    public List<Brand> findAllById(Iterable<Long> ids) {
        return brandJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id);
    }
}
