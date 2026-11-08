package ijiri.ijiriserver.user.member.application.service

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.transaction.TransactionHooks
import ijiri.ijiriserver.user.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.user.member.application.data.MemberRegisterResult
import ijiri.ijiriserver.user.member.application.data.MemberSignupCommand
import ijiri.ijiriserver.user.member.application.data.request.MemberUpdateRequest
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse
import ijiri.ijiriserver.user.member.application.port.`in`.ChangeMemberPasswordUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.CreateEmailMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMyProfileUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.RegisterSocialMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.UpdateMemberProfileUseCase
import ijiri.ijiriserver.user.member.application.port.out.MemberPort
import ijiri.ijiriserver.user.member.application.port.out.ProfileImagePort
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.entity.Role
import ijiri.ijiriserver.user.member.exception.MemberStatusCode
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ThreadLocalRandom

@Service
@Transactional(readOnly = true)
class MemberCommandService(
    private val memberPort: MemberPort,
    private val profileImagePort: ProfileImagePort,
    private val getMemberUseCase: GetMemberUseCase,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val findEmailMemberUseCase: FindEmailMemberUseCase,
) : UpdateMemberProfileUseCase,
    RegisterSocialMemberUseCase,
    CreateEmailMemberUseCase,
    ChangeMemberPasswordUseCase {

    // 새 프로필 사진은 service-media 가 이 회원이 PROFILE 용도로 올린 사진인지 확인한다 (아니면 INVALID_IMAGE 로 롤백)
    @Transactional
    override fun updateProfile(memberId: Long, request: MemberUpdateRequest): MemberResponse {
        val member = getMemberUseCase.getById(memberId)
        var nickname = member.nickname
        val requestedNickname = request.nickname
        if (requestedNickname != null && requestedNickname != nickname) {
            if (memberPort.existsByNickname(requestedNickname)) {
                throw CustomException(MemberStatusCode.DUPLICATE_NICKNAME)
            }
            nickname = requestedNickname
        }
        val previousUrl = member.profileImageUrl
        var profileImageUrl = previousUrl
        val profileImageKey = request.profileImageKey
        if (profileImageKey != null) {
            profileImageUrl = if (profileImageKey.isBlank()) null else attachProfileImage(memberId, profileImageKey)
        }
        val requestedStatusMessage = request.statusMessage
        val statusMessage = if (requestedStatusMessage == null) {
            member.statusMessage
        } else {
            emptyToNull(requestedStatusMessage)
        }

        member.updateProfile(nickname, profileImageUrl, statusMessage)
        if (previousUrl != null && previousUrl != profileImageUrl) {
            // 이전 사진은 커밋 뒤에 지운다. 롤백되면 DB 가 계속 가리키므로 남겨야 한다
            TransactionHooks.afterCommit { profileImagePort.delete(memberId, previousUrl) }
        }
        return getMyProfileUseCase.getMe(memberId)
    }

    @Transactional
    override fun registerIfAbsent(command: MemberRegisterCommand): MemberRegisterResult {
        val member = memberPort.findByProviderAndProviderMemberId(command.provider, command.providerMemberId)
            ?: return MemberRegisterResult(memberPort.save(toMember(command)), true)
        if (member.isWithdrawn()) {
            throw CustomException(MemberStatusCode.MEMBER_WITHDRAWN)
        }
        return MemberRegisterResult(member, false)
    }

    // 이메일 회원은 provider = EMAIL, providerMemberId = email 로 저장해
    // (provider, providerMemberId) 유니크로 중복을 막는다. 동시 가입 경합도 유니크 위반으로 걸러진다
    @Transactional
    override fun signup(command: MemberSignupCommand): Member {
        if (memberPort.existsByProviderAndProviderMemberId(Provider.EMAIL, command.email)) {
            throw CustomException(MemberStatusCode.DUPLICATE_EMAIL)
        }
        if (memberPort.existsByNickname(command.nickname)) {
            throw CustomException(MemberStatusCode.DUPLICATE_NICKNAME)
        }
        try {
            return memberPort.saveAndFlush(
                Member(
                    provider = Provider.EMAIL,
                    providerMemberId = command.email,
                    email = command.email,
                    nickname = command.nickname,
                    password = command.encodedPassword,
                    role = Role.USER,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw CustomException(duplicateOf(e))
        }
    }

    @Transactional
    override fun changePassword(email: String, encodedPassword: String): Long {
        val member = findEmailMemberUseCase.findEmailMember(email)
            ?.takeIf { found -> !found.isWithdrawn() }
            ?: throw CustomException(MemberStatusCode.MEMBER_NOT_FOUND)
        member.changePassword(encodedPassword)
        return member.id
    }

    // 저장소의 영구 키 이동은 DB 롤백으로 되돌릴 수 없으므로, 롤백되면 옮겨진 사진을 지워 같은 키로 다시 시도할 수 있게 한다
    private fun attachProfileImage(memberId: Long, imageKey: String): String {
        val attached = profileImagePort.attach(memberId, imageKey)
        TransactionHooks.afterRollback { profileImagePort.delete(memberId, attached.url) }
        return attached.url
    }

    // 사전 검사와 저장 사이에 같은 닉네임·이메일 가입이 먼저 끝나면 유니크 제약에 걸린다. 어느 제약인지로 코드를 고른다
    private fun duplicateOf(e: DataIntegrityViolationException): MemberStatusCode {
        val constraintName = (e.cause as? ConstraintViolationException)?.constraintName
        return if (constraintName == Member.NICKNAME_UNIQUE_CONSTRAINT) {
            MemberStatusCode.DUPLICATE_NICKNAME
        } else {
            MemberStatusCode.DUPLICATE_EMAIL
        }
    }

    private fun toMember(command: MemberRegisterCommand): Member {
        return Member(
            provider = command.provider,
            providerMemberId = command.providerMemberId,
            email = command.email,
            nickname = generateNickname(),
            profileImageUrl = command.profileImageUrl,
            role = Role.USER,
        )
    }

    // 소셜 계정의 이름(구글 name 은 실명)은 공개 커뮤니티에 노출되면 안 되므로 쓰지 않고 항상 자동 생성한다.
    // 나중에 PATCH /members/me 로 바꿀 수 있다
    private fun generateNickname(): String {
        repeat(MAX_NICKNAME_TRIES) {
            val candidate = randomNickname()
            if (!memberPort.existsByNickname(candidate)) {
                return candidate
            }
        }
        // 그래도 겹치면 저장할 때 유니크 제약에 걸려 OAuthService 가 한 번 더 시도한다
        return randomNickname()
    }

    private fun randomNickname(): String {
        // 접두사 5자 + 6자리 = 11자 (닉네임 최대 12자). 4자리(9천 개)로는 회원이 늘면 금방 겹친다
        return DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(100_000, 1_000_000)
    }

    private fun emptyToNull(value: String): String? {
        return value.ifEmpty { null }
    }

    companion object {
        private const val DEFAULT_NICKNAME_PREFIX = "이지리오너"
        private const val MAX_NICKNAME_TRIES = 10
    }
}
