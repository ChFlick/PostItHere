package app

import ConfigStore
import DatabaseStringSpec
import TestDbContainer
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.test.TestCase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.mindrot.jbcrypt.BCrypt
import run
import security.JwtConfig
import testConfig
import testUser

@KtorExperimentalAPI
@ExperimentalSerializationApi
class UsersControllerKtTest : DatabaseStringSpec() {
    private val jwtConfig = JwtConfig(testConfig)
    private var testDi: DI = DI {}
    private val configStore = mockk<ConfigStore>()
    private val nonExistingUser = User(email = "nonexisting@example.com", password = "somepswd")

    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)

        val client = TestDbContainer.client
        testDi = DI {
            bind<CoroutineDatabase>() with singleton { client }
            bind<UserService>() with singleton { MongoUserService(instance()) }
            bind<JwtConfig>() with singleton { jwtConfig }
            bind<ConfigStore>() with singleton { configStore }
        }

        val userColl = client.getCollection<User>("users")
        runBlocking {
            userColl.insertOne(
                User(
                    email = testUser.email,
                    password = BCrypt.hashpw(testUser.password, BCrypt.gensalt())
                )
            )
        }
    }

    init {
        "Login should fail with invalid password" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/login"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential(testUser.email, "notthepassword")))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Unauthorized
                req.response.content.shouldBeNull()
            }
        }

        "Login should fail with invalid email" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/login"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential("wrongemail@example.com", testUser.password)))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Unauthorized
                req.response.content.shouldBeNull()
            }
        }

        "Login should succeed with valid credentials" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/login"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential(testUser.email, testUser.password)))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.OK
                req.response.content.let {
                    it.shouldNotBeNull()
                    it.shouldNotBeBlank()
                }
            }
        }

        "Registration should succeed with valid data" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns true

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential(nonExistingUser.email, nonExistingUser.password)))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Created
            }
        }

        "Registration should be denied with deactivated config" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns false

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential(nonExistingUser.email, nonExistingUser.password)))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.ServiceUnavailable
            }
        }

        "Registration should be denied with duplicate email config" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns true

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(EmailPasswordCredential(testUser.email, testUser.password)))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.NotModified
            }
        }

        "Registration should fail with only email" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns true

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody("""{"email": "${nonExistingUser.email}"}""")
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.BadRequest
                req.response.content shouldBe "Field 'password' is required, but it was missing"
            }
        }

        "Registration should fail with only password" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns true

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody("""{"password": "${nonExistingUser.password}"}""")
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.BadRequest
                req.response.content shouldBe "Field 'email' is required, but it was missing"
            }
        }
    }
}
