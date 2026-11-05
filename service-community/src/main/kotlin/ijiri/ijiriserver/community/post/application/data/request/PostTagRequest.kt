package ijiri.ijiriserver.community.post.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PostTagRequest(
    @Schema(description = "parts 의 ref", example = "p1")
    @field:NotBlank
    val ref: String?,

    @Schema(description = "사진 너비에 대한 가로 위치 비율 (0~1)", example = "0.42")
    @field:NotNull @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val x: Double?,

    @Schema(description = "사진 높이에 대한 세로 위치 비율 (0~1)", example = "0.71")
    @field:NotNull @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val y: Double?,
)
