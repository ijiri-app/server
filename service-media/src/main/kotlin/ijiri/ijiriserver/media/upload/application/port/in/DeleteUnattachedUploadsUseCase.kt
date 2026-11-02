package ijiri.ijiriserver.media.upload.application.port.`in`

interface DeleteUnattachedUploadsUseCase {

    /**
     * 하루 안에 게시물·프로필에 연결되지 않은 업로드를 지우고 지운 수를 돌려준다.
     */
    fun deleteUnattached(): Int
}
