package ijiri.ijiriserver.media.upload.application.data.response

data class UploadResponse(
    val items: List<Item>,
) {

    data class Item(
        val imageKey: String,
        val uploadUrl: String,
        val expiresIn: Long,
    )
}
