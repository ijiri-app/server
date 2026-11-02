package ijiri.ijiriserver.global.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 저장소의 이미지 키와 공개 URL 을 서로 바꾼다. URL = public-base-url + "/" + 키.
 * 앱이 올리는 임시 키는 tmp/ 로 시작하고, 게시물·프로필에 연결되면 tmp/ 를 뗀 영구 키로 옮겨진다
 * (R2 수명 주기 규칙이 tmp/ 아래만 하루 뒤 지운다).
 */
@Component
class ImageUrlResolver(
    @Value("\${storage.public-base-url}") publicBaseUrl: String,
) {

    private val urlPrefix: String = "$publicBaseUrl/"

    fun urlOf(imageKey: String): String = urlPrefix + imageKey

    fun permanentKeyOf(uploadKey: String): String = uploadKey.removePrefix(TEMPORARY_PREFIX)

    // 소셜 프로필 사진처럼 우리 저장소가 아닌 URL 이면 null
    fun keyOf(url: String?): String? {
        if (url == null || !url.startsWith(urlPrefix)) {
            return null
        }
        return url.substring(urlPrefix.length)
    }

    companion object {
        private const val TEMPORARY_PREFIX = "tmp/"
    }
}
