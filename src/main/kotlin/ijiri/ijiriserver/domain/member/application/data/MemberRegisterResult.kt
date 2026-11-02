package ijiri.ijiriserver.domain.member.application.data

import ijiri.ijiriserver.domain.member.entity.Member

data class MemberRegisterResult(
    val member: Member,
    val isNewMember: Boolean,
)
