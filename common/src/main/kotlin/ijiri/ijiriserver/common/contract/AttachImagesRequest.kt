package ijiri.ijiriserver.common.contract

/**
 * POST /internal/images/attach. 이 회원이 이 용도로 받은 업로드 키여야 하고, 저장소에 올라온 파일을 확인한 뒤
 * 영구 키로 옮긴다 (아니면 INVALID_IMAGE). 요청 순서대로 돌려준다.
 */
data class AttachImagesRequest(
    val memberId: Long,
    val imageKeys: List<String>,
    val purpose: UploadPurpose,
)
