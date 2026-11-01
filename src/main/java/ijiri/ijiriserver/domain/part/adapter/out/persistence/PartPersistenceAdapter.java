package ijiri.ijiriserver.domain.part.adapter.out.persistence;

import ijiri.ijiriserver.domain.part.application.data.PartSuggestion;
import ijiri.ijiriserver.domain.part.application.port.out.PartPort;
import ijiri.ijiriserver.domain.part.entity.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartPersistenceAdapter implements PartPort {

    private final PartJpaRepository partJpaRepository;

    @Override
    public Optional<Part> findByBrandIdAndNormalizedName(Long brandId, String normalizedName) {
        return partJpaRepository.findByBrandIdAndNormalizedName(brandId, normalizedName);
    }

    @Override
    public Optional<Part> findByBrandIdIsNullAndNormalizedName(String normalizedName) {
        return partJpaRepository.findByBrandIdIsNullAndNormalizedName(normalizedName);
    }

    @Override
    public void insertIfAbsent(Long brandId, String name, String normalizedName, String category, LocalDateTime now) {
        partJpaRepository.insertIfAbsent(brandId, name, normalizedName, category, now);
    }

    @Override
    public void insertWithoutBrandIfAbsent(String name, String normalizedName, String category, LocalDateTime now) {
        partJpaRepository.insertWithoutBrandIfAbsent(name, normalizedName, category, now);
    }

    @Override
    public List<Part> search(String pattern, String category, long carModelId, int limit) {
        return partJpaRepository.search(pattern, category, carModelId, limit);
    }

    @Override
    public List<PartSuggestion> suggest(String keyword, String pattern, String category, long carModelId, int limit) {
        return partJpaRepository.suggest(keyword, pattern, category, carModelId, limit);
    }

    @Override
    public void addUseCount(Collection<Long> ids, int delta) {
        partJpaRepository.addUseCount(ids, delta);
    }

    @Override
    public List<Part> findAllById(Iterable<Long> ids) {
        return partJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<Part> findById(Long id) {
        return partJpaRepository.findById(id);
    }
}
