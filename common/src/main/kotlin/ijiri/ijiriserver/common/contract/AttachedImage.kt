package ijiri.ijiriserver.common.contract

/**
 * service-media 의 POST /internal/images/attach 응답 항목. 영구 키와 공개 URL.
 */
data class AttachedImage(
    val imageKey: String,
    val url: String,
)
