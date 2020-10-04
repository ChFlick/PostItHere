package app

import com.mongodb.WriteConcern
import io.ktor.auth.*
import kotlinx.serialization.*
import org.bson.types.ObjectId
import org.litote.kmongo.Id
import org.litote.kmongo.addToSet
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.id.MongoId
import org.mindrot.jbcrypt.BCrypt

@Serializable
data class ApiKey(
    val key: String,
    val usages: Int,
    val capacity: Int
)

@Serializable
data class User(
    @Contextual @SerialName("_id") @MongoId val id: Id<User>? = null,
    val email: String,
    val password: String,
    val apiKey: ApiKey? = null,
    val formIds: List<String>? = emptyList()
) : Principal

@Serializable
data class EmailPasswordCredential (val email: String, val password: String) : Credential

interface UserService {
    suspend fun getUserByEmail(email: String): User?

    suspend fun addUser(user: User): Boolean

    suspend fun addFormToUser(user: User, formId: String): Boolean

    suspend fun getUserById(userId: String): User?

    suspend fun getUserByCredentials(credential: EmailPasswordCredential): User?
}

class MongoUserService(database: CoroutineDatabase) : UserService {
    private val userCollection = database.getCollection<User>("users").withWriteConcern(WriteConcern.MAJORITY)

    override suspend fun getUserByEmail(email: String): User? {
        return userCollection.findOne(User::email eq email)
    }

    override suspend fun addUser(user: User): Boolean {
        val userWithHashedPassword = User(
            user.id,
            user.email,
            BCrypt.hashpw(user.password, BCrypt.gensalt()),
            user.apiKey,
            user.formIds
        )

        return userCollection.insertOne(userWithHashedPassword).wasAcknowledged()
    }

    override suspend fun addFormToUser(user: User, formId: String): Boolean {
        return userCollection.updateOne(
            User::email eq user.email,
            addToSet(User::formIds, formId)
        ).modifiedCount == 1L
    }

    override suspend fun getUserById(userId: String): User? {
        return userCollection.findOneById(ObjectId(userId))
    }

    override suspend fun getUserByCredentials(credential: EmailPasswordCredential): User? {
        val user = userCollection.findOne(User::email eq credential.email) ?: return null
        return if (BCrypt.checkpw(credential.password, user.password)) user else null
    }

}