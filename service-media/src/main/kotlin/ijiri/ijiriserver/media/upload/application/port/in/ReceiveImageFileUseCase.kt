package ijiri.ijiriserver.media.upload.application.port.`in`

interface ReceiveImageFileUseCase {

    /**
     * 업로드 URL 의 서명을 확인하고 파일을 저장한다.
     */
    fun receive(imageKey: String, expires: Long, signature: String, contentType: String, content: ByteArray)
}
