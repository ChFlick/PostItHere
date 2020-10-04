import app.User
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.litote.kmongo.coroutine.CoroutineDatabase

class JwtSecurityTest : StringSpec({
    val user = User(email = "test@example.com", password = "test")

    beforeEach {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val userColl = database.getCollection<User>("users")
            runBlocking {
                userColl.insertOne(user)
            }
        }
    }

    afterEach {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val userColl = database.getCollection<User>("users")
            runBlocking {
                userColl.deleteOneById(user.id!!)
            }
        }
    }

    "A Request should fail without a token" {
        withServer {
            val req = handleRequest {
                uri = "/secret"
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.Unauthorized
        }
    }

    "A Request should fail with an invalid token" {
        withServer {
            val req = handleRequest {
                uri = "/secret"
                addHeader("Authorization", "Bearer haofsi7yfa8ohfoahfa3784hfoa")
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.Unauthorized
        }
    }

    "A Request should succeed with a correct token" {
        withServer {
            val req = handleRequest {
                uri = "/secret"
                addJwtHeader(user)
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.OK
        }
    }
})