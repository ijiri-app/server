package ijiri.ijiriserver.domain.auth.token.application.port.in;

public interface RevokeMemberSessionsUseCase {

    void signOut(Long memberId);
}
