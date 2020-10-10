import app.FormService
import app.MongoUserService
import app.UserService
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.WriteConcern
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.config.ApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.serialization.json
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.event.Level
import routes.routes
import security.JwtConfig


@KtorExperimentalAPI
fun initDatabase(config: ApplicationConfig): CoroutineDatabase {
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

    val client = KMongo.createClient(mongoClientSettings).coroutine // use coroutine extension

    return client.getDatabase(config.property("mongodb.database").getString()) // normal java driver usage
}

@ExperimentalSerializationApi
@KtorExperimentalAPI
@SuppressWarnings("unused") // used in application.conf
fun Application.main() {
    run(DI {
        bind<CoroutineDatabase>() with singleton { initDatabase(environment.config) }
        bind<ConfigStore>() with singleton { ConfigStore(instance()) }
        bind<UserService>() with singleton { MongoUserService(instance()) }
        bind<FormService>() with singleton { FormService(instance()) }
        bind<JwtConfig>() with singleton { JwtConfig(environment.config) }
    })
}

@ExperimentalSerializationApi
@KtorExperimentalAPI
@SuppressWarnings("unused") // used in application.conf
fun Application.run(di: DI) {
    di {
        extend(di)
    }

    install(Authentication) {
        val userService by di().instance<UserService>()
        val jwtConfig by di().instance<JwtConfig>()
        val jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "test"
        val jwtRealm = environment.config.propertyOrNull("jwt.realm")?.getString() ?: "test"

        jwt {
            realm = jwtRealm
            verifier(jwtConfig.verifier)
            validate {
                if (it.payload.audience.contains(jwtAudience))
                    it.payload.subject.let { id -> userService.getUserById(id) }
                else
                    null
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

    install(StatusPages) {
        exception<SerializationException> { cause ->
            cause.message?.let { call.respond(HttpStatusCode.BadRequest, it) }
                ?: call.respond(HttpStatusCode.BadRequest)
        }
    }

    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.DEBUG
    }
    install(Routing) {
        routes()
    }
}