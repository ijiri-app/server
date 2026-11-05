package ijiri.ijiriserver.media.upload.adapter.out.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * presigned URL 은 네트워크 호출 없이 서명만 계산하므로 실제 R2 없이 확인할 수 있다.
 */
class R2ImageStorageClientTest {

    private val client = R2ImageStorageClient("account", "key-id", "secret", "ijiri")

    @AfterEach
    fun close() {
        client.close()
    }

    @Test
    fun 업로드_URL_은_버킷_키에_대한_PUT_서명이고_Content_Type_을_서명에_묶는다() {
        val url = client.createUploadUrl("tmp/posts/a.jpg", "image/jpeg", Duration.ofMinutes(10))

        assertThat(url).startsWith("https://account.r2.cloudflarestorage.com/ijiri/tmp/posts/a.jpg?")
        assertThat(url).contains("X-Amz-Expires=600")
        assertThat(url).containsPattern("X-Amz-SignedHeaders=[^&]*content-type")
    }
}
