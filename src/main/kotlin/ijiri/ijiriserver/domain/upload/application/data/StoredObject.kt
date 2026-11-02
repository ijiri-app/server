package ijiri.ijiriserver.domain.upload.application.data

/**
 * 저장소에 올라간 파일의 메타데이터 (HEAD).
 */
data class StoredObject(
    val size: Long,
    val contentType: String?,
)
