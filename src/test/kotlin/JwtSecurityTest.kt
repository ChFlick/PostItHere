import app.UserService
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import security.JwtConfig

class JwtSecurityTest : StringSpec({
    var testDi = DI {}
    val userService = mockk<UserService>()

    beforeEach {
        val jwtConfig = JwtConfig(testConfig)
        testDi = DI {
            bind<UserService>() with singleton { userService }
            bind<JwtConfig>() with singleton { jwtConfig }
        }
    }

    "A Request should fail without a token" {
        withTestApplication({ run(testDi) }) {
            val req = handleRequest {
                uri = "/secret"
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.Unauthorized
        }
    }

    "A Request should fail with an invalid token" {
        withTestApplication({ run(testDi) }) {
            val req = handleRequest {
                uri = "/secret"
                addHeader("Authorization", "Bearer haofsi7yfa8ohfoahfa3784hfoa")
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.Unauthorized
        }
    }

    "A Request should succeed with a correct token" {
        coEvery { userService.getUserById(testUser.id.toString()) } returns testUser

        withTestApplication({ run(testDi) }) {
            val req = handleRequest {
                uri = "/secret"
                addJwtHeader()
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.OK
        }
    }
})