package ijiri.ijiriserver.domain.auth.common.application.port.`in`

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent

interface UnlinkSocialBeforePurgeUseCase {

    fun unlinkBeforePurge(event: MemberPurgedEvent)
}
