import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.WriteConcern
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import org.bson.Document
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.text.toCharArray


fun HTML.index() {
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            +"Hello from Ktor"
        }
    }
}

@KtorExperimentalAPI
fun Application.main() {
    val config = HoconApplicationConfig(ConfigFactory.load("application.local.conf"))

    val mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(config.property("mongodb.connectionstring").getString()))
        .credential(MongoCredential.createCredential(
            config.property("mongodb.user").getString(),
            "admin",
            config.property("mongodb.password").getString().toCharArray()))
        .retryWrites(true)
        .writeConcern(WriteConcern.ACKNOWLEDGED)
        .build()

    val client = KMongo.createClient(mongoClientSettings).coroutine //use coroutine extension
    val database = client.getDatabase(config.property("mongodb.database").getString()) //normal java driver usage
    val col = database.getCollection<Any>("example") //KMongo extension method

    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        get("/") {
            call.respondHtml(HttpStatusCode.OK, HTML::index)
        }
        get("/postit") {
            call.application.environment.log.info(call.parameters.toString())
            run {
                col.insertOne(Document(call.parameters.toMap()))
            }
            call.respond("")
        }
        post("/postit") {
            call.application.environment.log.info(call.parameters.toString())
            run {
                col.insertOne(Document(call.parameters.toMap()))
            }
            call.respond("")
        }
    }
}