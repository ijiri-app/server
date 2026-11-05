package ijiri.ijiriserver.common.security

/**
 * nginx 가 service-user 의 GET /internal/auth/verify 결과로 붙여 주는 헤더. 클라이언트가 보낸 같은 이름의 헤더는
 * nginx 가 덮어쓰므로 서비스는 이 값을 그대로 믿는다 (서비스는 내부망에서만 접근된다).
 */
object GatewayHeaders {
    const val MEMBER_ID = "X-Member-Id"
    const val MEMBER_ROLE = "X-Member-Role"
}
