package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.PostPartSummary

interface GetPostPartSummariesUseCase {

    /**
     * 공개 게시물의 부품만 돌려준다 (숨김·삭제된 게시물은 빠진다).
     */
    fun getPostPartSummaries(postPartIds: Collection<Long>): Map<Long, PostPartSummary>
}
