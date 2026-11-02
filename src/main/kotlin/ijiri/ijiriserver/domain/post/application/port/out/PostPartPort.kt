package ijiri.ijiriserver.domain.post.application.port.out

import ijiri.ijiriserver.domain.post.entity.PostPart

interface PostPartPort {

    fun findAllById(ids: Iterable<Long>): List<PostPart>

    fun findById(id: Long): PostPart?
}
