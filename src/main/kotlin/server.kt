import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.WriteConcern
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import routes.routes
import security.JwtConfig
import service.FormService
import service.UserService
import kotlin.text.toCharArray


@KtorExperimentalAPI
fun initDatabase(): CoroutineDatabase {
    val config = HoconApplicationConfig(ConfigFactory.load("application.local.conf"))

    val mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(config.property("mongodb.connectionstring").getString()))
        .credential(
            MongoCredential.createCredential(
                config.property("mongodb.user").getString(),
                "admin",
                config.property("mongodb.password").getString().toCharArray()
            )
        )
        .retryWrites(true)
        .writeConcern(WriteConcern.ACKNOWLEDGED)
        .build()

    val client = KMongo.createClient(mongoClientSettings).coroutine //use coroutine extension

    return client.getDatabase(config.property("mongodb.database").getString()) //normal java driver usage
}

@ExperimentalSerializationApi
@KtorExperimentalAPI
@SuppressWarnings("unused") // used in application.conf
fun Application.main() {
    di {
        bind<CoroutineDatabase>() with singleton { initDatabase() }
        bind<UserService>() with singleton { UserService(instance()) }
        bind<FormService>() with singleton { FormService(instance()) }
    }
    
    val userService by di().instance<UserService>()

    install(Authentication) {
        jwt {
            verifier(JwtConfig.verifier)
            realm = "postithere"
            validate {
                it.payload.getClaim("id").asString()?.let { id -> userService.getUserById(id) }
            }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
        maxAgeInSeconds = 86400L
    }

    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        routes()
    }
}