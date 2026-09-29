package ijiri.ijiriserver.domain.member.dto;

/**
 * 이메일 회원가입 정보. password 는 이미 인코딩된 값이다.
 */
public record MemberSignupCommand(
        String email,
        String nickname,
        String encodedPassword
) {
}
