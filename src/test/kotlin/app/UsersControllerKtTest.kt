package app

import ConfigStore
import DatabaseStringSpec
import TestDbContainer
import addJwtHeader
import io.kotest.assertions.ktor.shouldHaveContent
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.test.TestCase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
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
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
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
    private lateinit var userColl: CoroutineCollection<User>

    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)

        val client = TestDbContainer.client
        testDi = DI {
            bind<CoroutineDatabase>() with singleton { client }
            bind<UserService>() with singleton { MongoUserService(instance()) }
            bind<JwtConfig>() with singleton { jwtConfig }
            bind<ConfigStore>() with singleton { configStore }
        }

        userColl = client.getCollection(USERS_COLLECTION)
        runBlocking {
            userColl.insertOne(
                User(
                    testUser.id,
                    testUser.email,
                    BCrypt.hashpw(testUser.password, BCrypt.gensalt())
                )
            )
        }
    }

    init {
        //  LOGIN
        ///////////////////
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

        "Login should succeed with valid credentials with json" {
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

        "Login should succeed with valid credentials with form data" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/login"
                    method = HttpMethod.Post
                    addHeader("Content-Type", ContentType.Application.FormUrlEncoded.toString())
                    setBody("""email=${testUser.email}&password=${testUser.password}""")
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.OK
                req.response.content.let {
                    it.shouldNotBeNull()
                    it.shouldNotBeBlank()
                }
            }
        }

        //  REGISTRATION
        ///////////////////
        "Registration should succeed with valid data" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns true

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(
                        Json.encodeToString(
                            EmailPasswordCredential(
                                nonExistingUser.email,
                                nonExistingUser.password
                            )
                        )
                    )
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Created
                runBlocking {
                    userColl.findOne(User::email eq testUser.email) shouldNotBe null
                }
            }
        }

        "Registration should be denied with deactivated config" {
            withTestApplication({ run(testDi) }) {
                coEvery { configStore.getValue("allowRegistration") } returns false

                val req = handleRequest {
                    uri = "/users/register"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(
                        Json.encodeToString(
                            EmailPasswordCredential(
                                nonExistingUser.email,
                                nonExistingUser.password
                            )
                        )
                    )
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

        //  USER FORMS
        ///////////////////
        "Adding a form to a user without authentication should return an authentication error" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/${testUser.id}/forms"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(Form(formId = "1234", name = "someForm")))
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Unauthorized
                req.response.content shouldBe null
            }
        }

        "Adding a form to a user without formId should return an error" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/${testUser.id}/forms"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody("""{"name":"123"}""")
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.BadRequest
                req.response shouldHaveContent
                        """Field 'formId' is required, but it was missing"""
            }
        }

        "Adding a form to a user without formName should return an error" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/${testUser.id}/forms"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody("""{"formId":"123"}""")
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.BadRequest
                req.response shouldHaveContent
                        """Field 'name' is required, but it was missing"""
            }
        }

        "Adding a form to a user should be successful" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/users/${testUser.id}/forms"
                    method = HttpMethod.Post
                    addHeader("Content-Type", "application/json")
                    setBody(Json.encodeToString(Form(formId = "1234", name = "someForm")))
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Created
                req.response.content shouldBe null
                runBlocking {
                    (userColl.findOne(User::id eq testUser.id)?.forms?.size ?: 0) shouldBe 1
                }
            }
        }
    }
}
