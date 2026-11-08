package ijiri.ijiriserver.user.auth.common.application.port.`in`

import ijiri.ijiriserver.user.member.event.MemberWithdrawnEvent

interface UnlinkSocialAfterWithdrawalUseCase {

    fun unlinkAfterWithdrawal(event: MemberWithdrawnEvent)
}
