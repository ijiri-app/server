package ijiri.ijiriserver.user.member.application.port.`in`

interface ChangeMemberPasswordUseCase {

    /**
     * 활성 이메일 회원의 비밀번호를 바꾸고 회원 id 를 돌려준다. 없으면 NOT_FOUND.
     */
    fun changePassword(email: String, encodedPassword: String): Long
}
