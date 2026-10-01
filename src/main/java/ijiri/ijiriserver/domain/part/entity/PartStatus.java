package ijiri.ijiriserver.domain.part.entity;

public enum PartStatus {
    VERIFIED,
    // 사용자가 새로 입력한 부품. 관리자가 확인하거나 기존 부품으로 합친다
    PENDING
}
