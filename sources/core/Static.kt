package core

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

object Static {
    val clock = Clock.System
    val env: Dotenv = dotenv { ignoreIfMissing = true }
    val timeZone = TimeZone.of("Europe/Samara")
}