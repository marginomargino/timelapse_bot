package core

import telegram.Bot

fun main() {

    System.setProperty("slf4j.internal.verbosity", "WARN") // https://github.com/qos-ch/slf4j/issues/422
    Bot.start()
}



