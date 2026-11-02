package ijiri.ijiriserver.domain.upload.application.port.`in`

interface DeleteUploadedImagesUseCase {

    /**
     * 업로드 기록과 파일을 지운다 (영구 키). 없는 키는 무시한다.
     */
    fun delete(imageKeys: Collection<String>)
}
