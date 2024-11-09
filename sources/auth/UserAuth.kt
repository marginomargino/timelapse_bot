package auth

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserAuth(
    val chatId: Long,
    val timestamp: Instant,
)
