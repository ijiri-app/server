package ijiri.ijiriserver.media.upload.application.port.`in`

interface DeleteMemberImagesUseCase {

    /**
     * 이 회원의 사진을 공개 URL 또는 영구 키로 지운다 (파일 포함). 우리 저장소가 아닌 URL, 다른 회원의 사진,
     * 없는 사진은 무시한다.
     */
    fun delete(memberId: Long, imageUrls: Collection<String>, imageKeys: Collection<String>)

    /**
     * 회원의 업로드 기록과 파일을 모두 지운다 (영구 삭제). 여러 번 실행돼도 안전하다.
     */
    fun deleteAll(memberId: Long)
}
