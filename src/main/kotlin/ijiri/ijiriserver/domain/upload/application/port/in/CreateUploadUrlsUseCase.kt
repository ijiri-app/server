package ijiri.ijiriserver.domain.upload.application.port.`in`

import ijiri.ijiriserver.domain.upload.application.data.request.UploadUrlRequest
import ijiri.ijiriserver.domain.upload.application.data.response.UploadResponse

interface CreateUploadUrlsUseCase {

    fun createUploadUrls(memberId: Long, request: UploadUrlRequest): UploadResponse
}
