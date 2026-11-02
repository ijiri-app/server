package ijiri.ijiriserver.domain.member.entity

// APPLE 은 아직 토큰 검증 구현이 없어 로그인하면 UNSUPPORTED_PROVIDER
enum class Provider {
    EMAIL,
    KAKAO,
    GOOGLE,
    APPLE,
}
