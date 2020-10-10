import com.mongodb.MongoException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

@Serializable
data class BooleanConfig(val key: String, val value: Boolean)

class ConfigStore(database: CoroutineDatabase) {
    private val booleanConfig = database.getCollection<BooleanConfig>("config")

    init {
        runBlocking {
            booleanConfig.ensureUniqueIndex(BooleanConfig::key)
        }
    }

    suspend fun getValue(key: String, default: Boolean = false): Boolean = try {
        booleanConfig.findOne(BooleanConfig::key eq key)?.value ?: default
    } catch (e: MongoException) {
        default
    }
}
