package ijiri.ijiriserver.common.contract

/**
 * POST /internal/images/delete. 이 회원이 올린 사진 중 주어진 공개 URL 또는 영구 키의 업로드 기록과 파일을 지운다.
 * 우리 저장소가 아닌 URL(소셜 프로필 사진)이나 다른 회원의 사진, 없는 사진은 무시한다.
 */
data class DeleteImagesRequest(
    val memberId: Long,
    val imageUrls: List<String> = emptyList(),
    val imageKeys: List<String> = emptyList(),
)
