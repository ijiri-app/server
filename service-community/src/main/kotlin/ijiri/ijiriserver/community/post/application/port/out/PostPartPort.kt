package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.community.post.entity.PostPart

interface PostPartPort {

    fun findAllById(ids: Iterable<Long>): List<PostPart>

    fun findById(id: Long): PostPart?
}
