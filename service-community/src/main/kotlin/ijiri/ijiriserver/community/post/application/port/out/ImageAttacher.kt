package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.common.contract.AttachedImage

/**
 * 사진 파일은 service-media 에 있다.
 */
interface ImageAttacher {

    /**
     * 이 회원이 게시물 용도로 올린 사진을 영구 키로 옮기고 요청 순서대로 돌려준다. 아니면 INVALID_IMAGE.
     */
    fun attach(memberId: Long, imageKeys: List<String>): List<AttachedImage>

    /**
     * 영구 키의 사진을 지운다 (게시물 저장이 롤백됐을 때). 없는 키는 무시된다.
     */
    fun delete(memberId: Long, imageKeys: List<String>)
}
