package ijiri.ijiriserver.domain.report.entity;

public enum ReportStatus {
    PENDING,
    // 게시물 숨김 또는 사용자 정지로 처리됨
    ACTIONED,
    DISMISSED
}
