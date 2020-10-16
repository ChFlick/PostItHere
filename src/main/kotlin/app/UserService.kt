package app

import com.mongodb.WriteConcern
import io.ktor.auth.Credential
import io.ktor.auth.Principal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.litote.kmongo.Id
import org.litote.kmongo.addToSet
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.id.MongoId
import org.mindrot.jbcrypt.BCrypt

const val USERS_COLLECTION = "users"

@Serializable
data class ApiKey(
    val key: String,
    val usages: Int,
    val capacity: Int
)

@Serializable
data class Form(
    @Contextual @SerialName("_id") @MongoId val id: Id<Form>? = null,
    val formId: String,
    val name: String
)

@Serializable
data class User(
    @Contextual @SerialName("_id") @MongoId val id: Id<User>? = null,
    val email: String,
    val password: String,
    val apiKey: ApiKey? = null,
    val forms: List<Form>? = emptyList()
) : Principal

@Serializable
data class EmailPasswordCredential(val email: String, val password: String) : Credential

interface UserService {
    suspend fun getUserByEmail(email: String): User?

    suspend fun addUser(user: User): Boolean

    suspend fun addFormToUser(userId: String, form: Form): Boolean

    suspend fun isFormIdAvailable(formId: String): Boolean

    suspend fun getUserById(userId: String): User?

    suspend fun getUserByCredentials(credential: EmailPasswordCredential): User?
}

class MongoUserService(database: CoroutineDatabase) : UserService {
    private val userCollection = database.getCollection<User>(USERS_COLLECTION)
        .withWriteConcern(WriteConcern.MAJORITY)

    init {
        runBlocking {
            userCollection.ensureUniqueIndex(User::email)
        }
    }

    override suspend fun getUserByEmail(email: String): User? {
        return userCollection.findOne(User::email eq email)
    }

    override suspend fun addUser(user: User): Boolean {
        val userWithHashedPassword = User(
            user.id,
            user.email,
            BCrypt.hashpw(user.password, BCrypt.gensalt()),
            user.apiKey,
            user.forms
        )

        return userCollection.insertOne(userWithHashedPassword).wasAcknowledged()
    }

    override suspend fun addFormToUser(userId: String, form: Form): Boolean {
        return userCollection.updateOne(
            User::id eq ObjectId(userId),
            addToSet(User::forms, form)
        ).modifiedCount == 1L
    }

    override suspend fun isFormIdAvailable(formId: String): Boolean {
        return userCollection.findOne("""{"forms.formId":"$formId"}""") != null
    }

    override suspend fun getUserById(userId: String): User? {
        return userCollection.findOneById(ObjectId(userId))
    }

    override suspend fun getUserByCredentials(credential: EmailPasswordCredential): User? =
        userCollection.findOne(User::email eq credential.email)?.takeIf {
            BCrypt.checkpw(credential.password, it.password)
        }
}