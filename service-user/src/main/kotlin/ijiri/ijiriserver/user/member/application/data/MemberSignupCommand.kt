package ijiri.ijiriserver.user.member.application.data

/**
 * 이메일 회원가입 정보. password 는 이미 인코딩된 값이다.
 */
data class MemberSignupCommand(
    val email: String,
    val nickname: String,
    val encodedPassword: String,
)
