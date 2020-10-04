import app.User
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import org.slf4j.LoggerFactory
import security.JwtConfig

var testUser = User(email = "test@example.com", password = "test")
val testConfig = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))

fun TestApplicationRequest.addJwtHeader(user: User?) = addHeader("Authorization", "Bearer ${getToken(user)}")
fun getToken(user: User?) = JwtConfig(testConfig).makeToken(user ?: testUser)

fun getTestEnvironment() = applicationEngineEnvironment {
    config = testConfig
    log = LoggerFactory.getLogger("ktor.test")
}

fun <R> withTestApplication(moduleFunction: Application.() -> Unit, test: TestApplicationEngine.() -> R): R {
    return withApplication(getTestEnvironment(), test = test)
}

fun withServer(test: TestApplicationEngine.() -> Unit) = withTestApplication({ main() }, test)