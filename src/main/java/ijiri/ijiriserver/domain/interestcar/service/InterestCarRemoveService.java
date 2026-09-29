package ijiri.ijiriserver.domain.interestcar.service;

/**
 * 회원 탈퇴 시 관심 차종 전체 삭제
 */
public interface InterestCarRemoveService {

    void removeAll(Long memberId);
}
