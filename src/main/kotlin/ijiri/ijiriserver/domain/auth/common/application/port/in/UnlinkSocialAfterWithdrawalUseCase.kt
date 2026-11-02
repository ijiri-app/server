package ijiri.ijiriserver.domain.auth.common.application.port.`in`

import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent

interface UnlinkSocialAfterWithdrawalUseCase {

    fun unlinkAfterWithdrawal(event: MemberWithdrawnEvent)
}
