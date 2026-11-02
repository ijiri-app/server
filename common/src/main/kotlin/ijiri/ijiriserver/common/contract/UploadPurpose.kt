package ijiri.ijiriserver.common.contract

enum class UploadPurpose(
    // 임시 키 앞부분. 용도가 다른 곳에 쓰이지 않도록 연결할 때 확인한다
    val keyPrefix: String,
) {
    POST("tmp/posts/"),
    PROFILE("tmp/profiles/"),
}
