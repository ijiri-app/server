package ijiri.ijiriserver.domain.upload.application.port.`in`

import ijiri.ijiriserver.domain.upload.application.data.ImageFileContent

interface LoadImageFileUseCase {

    fun load(imageKey: String): ImageFileContent
}
