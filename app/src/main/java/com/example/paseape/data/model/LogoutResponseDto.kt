data class LogoutResponseDto(
    val statusCode: Int,
    val code: String,
    val message: String,
    val response: LogoutData?
)

data class LogoutData(
    val correo: String?,
    val mensaje: String?,
    val requiereRevocacionGoogle: Boolean
)