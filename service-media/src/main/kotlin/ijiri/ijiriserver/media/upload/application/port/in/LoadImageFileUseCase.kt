package ijiri.ijiriserver.media.upload.application.port.`in`

import ijiri.ijiriserver.media.upload.application.data.ImageFileContent

interface LoadImageFileUseCase {

    fun load(imageKey: String): ImageFileContent
}
