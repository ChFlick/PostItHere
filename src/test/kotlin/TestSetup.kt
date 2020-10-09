import app.User
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import org.litote.kmongo.Id
import org.litote.kmongo.id.IdGenerator
import org.slf4j.LoggerFactory
import security.JwtConfig

var testUser = User(IdGenerator.defaultGenerator.generateNewId(), "test@example.com", "test")
val testConfig = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))

fun TestApplicationRequest.addJwtHeader() = addHeader("Authorization", "Bearer ${getToken()}")
fun getToken() = JwtConfig(testConfig).makeToken(testUser)
