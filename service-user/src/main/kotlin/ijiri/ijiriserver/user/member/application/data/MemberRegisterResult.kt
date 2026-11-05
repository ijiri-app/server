package ijiri.ijiriserver.user.member.application.data

import ijiri.ijiriserver.user.member.entity.Member

data class MemberRegisterResult(
    val member: Member,
    val isNewMember: Boolean,
)
