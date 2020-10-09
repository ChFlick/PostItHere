import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.WriteConcern
import com.mongodb.reactivestreams.client.MongoClient
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.testcontainers.containers.MongoDBContainer

class TestDbContainer : MongoDBContainer() {
    companion object {
        private lateinit var instance: TestDbContainer
        private lateinit var mongoClientSettings: MongoClientSettings
        lateinit var client: CoroutineDatabase

        fun start() {
            instance = TestDbContainer()
            instance.dockerImageName = "mongo:4.2.10"
            instance.start()

            mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(
                    ConnectionString("mongodb://${instance.containerIpAddress}:${instance.firstMappedPort}")
                )
                .build()
            client = KMongo.createClient(mongoClientSettings).coroutine.getDatabase("test")
        }

        fun stop() {
            instance.stop()
        }
    }
}

abstract class DatabaseStringSpec(body: StringSpec.() -> Unit = {}) : StringSpec(body) {
    override fun beforeEach(testCase: TestCase) {
        TestDbContainer.start()
    }

    override fun afterEach(testCase: TestCase, result: TestResult) {
        TestDbContainer.stop()
    }
}
