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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files


object Bot {

    private val userPasswordStatus = mutableMapOf<ChatId, Boolean>()

    private val bot: Bot = bot {
        token = env["BOT_TOKEN"]
        timeout = 180
        logLevel = LogLevel.Error
        dispatch {
            command("start") {
                val chatId = ChatId.fromId(message.chat.id)
                userPasswordStatus[chatId] = false
                bot.sendMessage(chatId = chatId, text = "Please enter the password to access the bot:")
            }
            command("full") {
                processTimelapseTask(Task.Full)
            }
            command("today") {
                processTimelapseTask(Task.Today)
            }
            command("yesterday") {
                processTimelapseTask(Task.Yesterday)
            }
            telegramError {
                println("Error: ${error.getType()}: ${error.getErrorMessage()}")
            }

            text {
                val text = message.text
                if (text != null && !text.startsWith("/")) {
                    when {
                        text.startsWith("$env[ADMIN_PREFIX] ") -> handleBashCommand(
                            message = text.removePrefix("$env[ADMIN_PREFIX] ").trim(),
                            chatId = ChatId.fromId(message.chat.id),
                            userId = message.from?.id
                        )

                        else -> handleIncomingMessage(text, ChatId.fromId(message.chat.id), message.messageId)
                    }
                }
            }
        }
    }


    private fun handleBashCommand(message: String, chatId: ChatId, userId: Long?) {
        if (env["ADMIN_ID"].toLong() != userId || message.isBlank())
            return

        try {
            when {
                message.startsWith("get ") -> {
                    val filename = message.removePrefix("get ").trim()
                    if (filename.isNotEmpty()) {
                        val filePath = "${env["HOME_PATH"]}/$filename"
                        val file = File(filePath)
                        if (file.exists()) {
                            withTempFile(file) {
                                bot.sendDocument(chatId = chatId, document = TelegramFile.ByFile(this))
                            }
                        } else {
                            bot.sendMessage(chatId = chatId, text = "File not found: $filename")
                        }
                    } else {
                        bot.sendMessage(chatId = chatId, text = "Please provide a filename.")
                    }
                }

                else -> {
                    val process = ProcessBuilder("/bin/bash", "-c", message).start()
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                    val output = StringBuilder()
                    reader.forEachLine { output.append(it).append("\n") }

                    val errorOutput = StringBuilder()
                    errorReader.forEachLine { errorOutput.append(it).append("\n") }

                    if (errorOutput.isNotEmpty()) {
                        output.append("Error:\n$errorOutput")
                    }

                    process.waitFor()
                    output.toString().trim()

                    bot.sendMessage(
                        chatId = chatId,
                        text = output.toString().takeIf { it.isNotBlank() } ?: "<empty output>")
                }
            }
        } catch (e: Exception) {
            println("Failed to execute command: ${e.message}")
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


    private fun CommandHandlerEnvironment.processTimelapseTask(task: Task) {
        val chatId = ChatId.fromId(message.chat.id)

        if (!userPasswordStatus.getOrDefault(chatId, false)) {
            bot.sendMessage(chatId = chatId, text = "You must enter the password first.")
            return
        }

        processTimelapseTask(chatId, task)
    }


    fun processTimelapseTask(chatId: ChatId, task: Task) {
        val actionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                bot.sendChatAction(chatId = chatId, action = ChatAction.UPLOAD_VIDEO)
                delay(2000)
            }
        }

        try {
            fun dateString(date: LocalDate) =
                String.format("%04d%02d%02d", date.year, date.monthNumber, date.dayOfMonth)

            val today = clock.todayIn(timeZone)
            val filename = when (task) {
                Task.Full -> "timelapse_full_${dateString(today)}.mp4"
                Task.Today -> "timelapse_${dateString(today)}.mp4"
                Task.Yesterday -> "timelapse_${dateString(today.minus(1, DateTimeUnit.DAY))}.mp4"
            }

            withTempFile(File("${env["HOME_PATH"]}${env["RELATIVE_TIMELAPSE_PATH"]}/$filename")) {
                bot.sendVideo(chatId = chatId, video = TelegramFile.ByFile(this))
            }
        } catch (e: Exception) {
            println("Error sending file: ${e.message}")
            bot.sendMessage(chatId = chatId, text = "There was an issue sending the video. Please try again later.")
        } finally {
            actionJob.cancel()
        }
    }


    fun start() {
        bot.startPolling()
    }


    private fun withTempFile(file: File, block: File.() -> Unit) {
        val tempFile = Files.createTempFile(file.nameWithoutExtension, file.extension).toFile()
        try {
            file.copyTo(tempFile, overwrite = true)
            block(file)
        } finally {
            tempFile.delete()
        }
    }
}
