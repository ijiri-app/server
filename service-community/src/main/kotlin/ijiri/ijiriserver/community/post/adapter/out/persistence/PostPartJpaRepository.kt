package ijiri.ijiriserver.community.post.adapter.out.persistence

import ijiri.ijiriserver.community.post.entity.PostPart
import org.springframework.data.jpa.repository.JpaRepository

interface PostPartJpaRepository : JpaRepository<PostPart, Long>
