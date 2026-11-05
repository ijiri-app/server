package ijiri.ijiriserver.media.upload.application.port.`in`

import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.UploadPurpose

interface AttachUploadedImagesUseCase {

    /**
     * 이 회원이 이 용도로 받은 업로드 키이고, 저장소에 10MB 이하의 JPEG·WebP 파일이 올라와 있어야 한다
     * (아니면 INVALID_IMAGE). 영구 키로 옮기고 요청 순서대로 돌려준다.
     */
    fun attach(memberId: Long, imageKeys: List<String>, purpose: UploadPurpose): List<AttachedImage>
}
