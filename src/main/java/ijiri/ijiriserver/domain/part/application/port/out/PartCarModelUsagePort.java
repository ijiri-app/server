package ijiri.ijiriserver.domain.part.application.port.out;

public interface PartCarModelUsagePort {

    void add(Long partId, Long carModelId, int delta);
}
