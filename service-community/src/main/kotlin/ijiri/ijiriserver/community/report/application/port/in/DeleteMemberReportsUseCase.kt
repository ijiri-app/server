package ijiri.ijiriserver.community.report.application.port.`in`

interface DeleteMemberReportsUseCase {

    /**
     * 회원이 한 신고와 받은 신고를 모두 지운다 (영구 삭제).
     */
    fun deleteAll(memberId: Long)
}
