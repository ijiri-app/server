package ijiri.ijiriserver.domain.ownedcar.application.port.in;

public interface DeleteOwnedCarUseCase {

    /**
     * 게시물이 연결된 차량은 지우지 않고 PAST 로 바꾼다.
     */
    void delete(Long memberId, Long ownedCarId);
}
