package ijiri.ijiriserver.community.report.entity

enum class ReportStatus {
    PENDING,

    // 게시물 숨김 또는 사용자 정지로 처리됨
    ACTIONED,
    DISMISSED,
}
