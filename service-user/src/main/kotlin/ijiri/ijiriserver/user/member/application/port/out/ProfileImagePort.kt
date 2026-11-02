package ijiri.ijiriserver.user.member.application.port.out

import ijiri.ijiriserver.common.contract.AttachedImage

/**
 * 프로필 사진 파일은 service-media 가 가진다.
 */
interface ProfileImagePort {

    /**
     * 이 회원이 PROFILE 용도로 받은 업로드 키를 영구 키로 옮기고 공개 URL 을 돌려준다. 아니면 INVALID_IMAGE.
     */
    fun attach(memberId: Long, imageKey: String): AttachedImage

    /**
     * 공개 URL 의 사진을 지운다. 우리 저장소가 아니거나(소셜 프로필 사진) 다른 회원의 사진이면 무시된다.
     */
    fun delete(memberId: Long, imageUrl: String)
}
