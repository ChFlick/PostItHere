import app.User
import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.testing.TestApplicationRequest
import org.litote.kmongo.id.IdGenerator
import security.JwtConfig

var testUser = User(IdGenerator.defaultGenerator.generateNewId(), "test@example.com", "test")
val testConfig = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))

fun TestApplicationRequest.addJwtHeader() = addHeader("Authorization", "Bearer ${getToken()}")
fun getToken() = JwtConfig(testConfig).makeToken(testUser)
