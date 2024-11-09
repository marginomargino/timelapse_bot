package core

import auth.UserAuth
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.singleOrNull
import kotlin.jvm.java

object DB {
    val client = MongoClient.create(Static.env["DB_URL"])
    val database = client.getDatabase(Static.env["DB_NAME"])
    val authenticationCollection = database.getCollection(
        Static.env["DB_COLLECTION_NAME"],
        UserAuth::class.java
    )

    suspend fun fetchUserAuth(chatId: Long): UserAuth? =
        authenticationCollection
            .find()
            .filter(Filters.eq("chatId", chatId))
            .singleOrNull()
            ?.takeIf { it.timestamp + Static.tokenValidityPeriod > Static.clock.now() }


    suspend fun saveUserAuth(chatId: Long) {
        authenticationCollection.updateOne(
            filter = Filters.eq("chatId", chatId),
            update = Updates.combine(
                Updates.set("timestamp", Static.clock.now()),
            ),
            options = UpdateOptions().upsert(true)
        )
    }
}
