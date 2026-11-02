package ijiri.ijiriserver.user.ownedcar.application.port.out

import ijiri.ijiriserver.user.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus

interface OwnedCarPort {

    fun findAllByMemberIdOrderByIdAsc(memberId: Long): List<OwnedCar>

    fun findByIdAndMemberId(id: Long, memberId: Long): OwnedCar?

    fun countByMemberIdAndStatus(memberId: Long, status: OwnedCarStatus): Long

    fun deleteAllByMemberIdInBulk(memberId: Long)

    fun delete(entity: OwnedCar)

    fun findAllById(ids: Iterable<Long>): List<OwnedCar>

    fun findById(id: Long): OwnedCar?

    fun save(entity: OwnedCar): OwnedCar
}
