package ijiri.ijiriserver.domain.part.application.port.out;

import ijiri.ijiriserver.domain.part.application.data.PartSuggestion;
import ijiri.ijiriserver.domain.part.entity.Part;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartPort {

    Optional<Part> findByBrandIdAndNormalizedName(Long brandId, String normalizedName);

    Optional<Part> findByBrandIdIsNullAndNormalizedName(String normalizedName);

    void insertIfAbsent(Long brandId, String name, String normalizedName, String category, LocalDateTime now);

    void insertWithoutBrandIfAbsent(String name, String normalizedName, String category, LocalDateTime now);

    List<Part> search(String pattern, String category, long carModelId, int limit);

    List<PartSuggestion> suggest(String keyword, String pattern, String category, long carModelId, int limit);

    void addUseCount(Collection<Long> ids, int delta);

    List<Part> findAllById(Iterable<Long> ids);

    Optional<Part> findById(Long id);
}
