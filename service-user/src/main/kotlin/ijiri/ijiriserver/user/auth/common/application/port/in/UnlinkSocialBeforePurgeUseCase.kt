package ijiri.ijiriserver.user.auth.common.application.port.`in`

import ijiri.ijiriserver.user.member.event.MemberPurgedEvent

interface UnlinkSocialBeforePurgeUseCase {

    fun unlinkBeforePurge(event: MemberPurgedEvent)
}
