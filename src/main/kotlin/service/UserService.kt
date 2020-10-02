package service

import com.mongodb.WriteConcern
import kotlinx.serialization.Serializable
import org.litote.kmongo.addToSet
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

@Serializable
data class ApiKey(
    val key: String,
    val usages: Int,
    val capacity: Int
)

@Serializable
data class User(
    val email: String,
    val password: String,
    val apiKey: ApiKey,
    val formIds: List<String>
)

class UserService(database: CoroutineDatabase) {
    private val userCollection = database.getCollection<User>().withWriteConcern(WriteConcern.MAJORITY)

    suspend fun getUserByEmail(email: String): User? {
        return userCollection.findOne(User::email eq email)
    }

    suspend fun addUser(user: User): Boolean {
        return userCollection.insertOne(user).wasAcknowledged()
    }

    suspend fun addFormToUser(user: User, formId: String): Boolean {
        return userCollection.updateOne(User::email eq user.email, addToSet(User::formIds, formId)).modifiedCount == 1L
    }

}