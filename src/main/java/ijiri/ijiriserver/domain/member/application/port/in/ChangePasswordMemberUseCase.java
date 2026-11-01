package ijiri.ijiriserver.domain.member.application.port.in;

public interface ChangePasswordMemberUseCase {

    /**
     * 활성 이메일 회원의 비밀번호를 바꾸고 회원 id 를 돌려준다. 없으면 NOT_FOUND.
     */
    Long changePassword(String email, String encodedPassword);
}
