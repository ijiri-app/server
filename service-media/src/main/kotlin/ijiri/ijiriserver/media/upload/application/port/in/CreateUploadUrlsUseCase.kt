package ijiri.ijiriserver.media.upload.application.port.`in`

import ijiri.ijiriserver.media.upload.application.data.request.UploadUrlRequest
import ijiri.ijiriserver.media.upload.application.data.response.UploadResponse

interface CreateUploadUrlsUseCase {

    fun createUploadUrls(memberId: Long, request: UploadUrlRequest): UploadResponse
}
