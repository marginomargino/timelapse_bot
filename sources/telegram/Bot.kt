package telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.logging.LogLevel
import core.Static.clock
import core.Static.env
import core.Static.timeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.io.File


object Bot {

    private val userPasswordStatus = mutableMapOf<ChatId, Boolean>()

    private val bot: Bot = bot {
        token = env["BOT_TOKEN"]
        logLevel = LogLevel.Error
        dispatch {
            command("start") {
                val chatId = ChatId.fromId(message.chat.id)
                userPasswordStatus[chatId] = false
                bot.sendMessage(chatId = chatId, text = "Please enter the password to access the bot:")
            }
            command("full") {
                process(Task.Full)
            }
            command("today") {
                process(Task.Today)
            }
            command("yesterday") {
                process(Task.Yesterday)
            }
            telegramError {
                println(error.getErrorMessage())
            }

            text {
                val text = message.text
                if (text != null && !text.startsWith("/")) {
                    handleIncomingMessage(text, ChatId.fromId(message.chat.id), message.messageId)
                }
            }
        }
    }


    private fun handleIncomingMessage(message: String?, chatId: ChatId, messageId: Long) {
        if (userPasswordStatus[chatId] != true) {
            if (message == env["USER_PWD"]) {
                userPasswordStatus[chatId] = true
                bot.sendMessage(chatId = chatId, text = "Access granted. You can now use the bot.")
            } else {
                bot.sendMessage(chatId = chatId, text = "Incorrect password. Please try again.")
            }
            bot.deleteMessage(chatId, messageId)
        }
    }


    private fun CommandHandlerEnvironment.process(task: Task) {
        val chatId = ChatId.fromId(message.chat.id)

        if (!userPasswordStatus.getOrDefault(chatId, false)) {
            bot.sendMessage(chatId = chatId, text = "You must enter the password first.")
            return
        }

        process(chatId, task)
    }


    fun process(chatId: ChatId, task: Task) {
        try {
            bot.sendChatAction(
                chatId = chatId,
                action = ChatAction.UPLOAD_VIDEO
            )

            fun dateString(date: LocalDate) =
                String.format("%04d%02d%02d", date.year, date.monthNumber, date.dayOfMonth)

            val today = clock.todayIn(timeZone)
            val filename = when (task) {
                Task.Full -> "timelapse_full_${dateString(today)}.mp4"
                Task.Today -> "timelapse_${dateString(today)}.mp4"
                Task.Yesterday -> "timelapse_${dateString(today.minus(1, DateTimeUnit.DAY))}.mp4"
            }

            val file = File("${env["FILE_PATH"]}/$filename")

            bot.sendVideo(
                chatId = chatId,
                video = TelegramFile.ByFile(file),
            )

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }


    fun start() {
        bot.startPolling()
    }
}
