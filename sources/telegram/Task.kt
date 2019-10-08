package telegram


sealed interface Task {
    object Full : Task
    object Today : Task
    object Yesterday : Task
}
