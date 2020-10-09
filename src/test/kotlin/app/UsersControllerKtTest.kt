package app

import BooleanConfig
import com.mongodb.client.model.UpdateOptions
import DatabaseStringSpec
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.test.TestCase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setValue
import org.mindrot.jbcrypt.BCrypt
import run
import security.JwtConfig
import testConfig
import testUser

val jwtConfig = JwtConfig(testConfig)
var testDi: DI = DI {}

class UsersControllerKtTest : DatabaseStringSpec() {
    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)

        val client = TestDbContainer.client
        testDi = DI {
            bind<CoroutineDatabase>() with singleton { client }
            bind<UserService>() with singleton { MongoUserService(instance()) }
            bind<JwtConfig>() with singleton { jwtConfig }
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
    }

    "Registration should succeed with valid data" {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val configColl = database.getCollection<BooleanConfig>("config")
            runBlocking {
                configColl.updateOne(
                    BooleanConfig::key eq "allowRegistration",
                    setValue(BooleanConfig::value, true),
                    UpdateOptions().upsert(true)
                )
            }

            val req = handleRequest {
                uri = "/users/register"
                method = HttpMethod.Post
                addHeader("Content-Type", "application/json")
                setBody(Json.encodeToString(EmailPasswordCredential(testUser.email, testUser.password)))
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.Created
        }
    }

    "Registration should be denied with deactivated config" {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val configColl = database.getCollection<BooleanConfig>("config")
            runBlocking {
                configColl.updateOne(
                    BooleanConfig::key eq "allowRegistration",
                    setValue(BooleanConfig::value, false),
                    UpdateOptions().upsert(true)
                )
            }

            val req = handleRequest {
                uri = "/users/register"
                method = HttpMethod.Post
                addHeader("Content-Type", "application/json")
                setBody(Json.encodeToString(EmailPasswordCredential(testUser.email, testUser.password)))
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.ServiceUnavailable
        }
    }

    "Registration should be denied with duplicate email config" {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val configColl = database.getCollection<BooleanConfig>("config")
            val userColl = database.getCollection<User>("users")

            runBlocking {
                configColl.updateOne(
                    BooleanConfig::key eq "allowRegistration",
                    setValue(BooleanConfig::value, true),
                    UpdateOptions().upsert(true)
                )

                userColl.insertOne(
                    User(
                        email = testUser.email,
                        password = testUser.password
                    )
                )
            }

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
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val configColl = database.getCollection<BooleanConfig>("config")

            runBlocking {
                configColl.updateOne(
                    BooleanConfig::key eq "allowRegistration",
                    setValue(BooleanConfig::value, true),
                    UpdateOptions().upsert(true)
                )
            }

            val req = handleRequest {
                uri = "/users/register"
                method = HttpMethod.Post
                addHeader("Content-Type", "application/json")
                setBody("""{"email": "email@example.com"}""")
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.BadRequest
            req.response.content shouldBe "Field 'password' is required, but it was missing"
        }
    }

    "Registration should fail with only password" {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val configColl = database.getCollection<BooleanConfig>("config")

            runBlocking {
                configColl.updateOne(
                    BooleanConfig::key eq "allowRegistration",
                    setValue(BooleanConfig::value, true),
                    UpdateOptions().upsert(true)
                )
            }

            val req = handleRequest {
                uri = "/users/register"
                method = HttpMethod.Post
                addHeader("Content-Type", "application/json")
                setBody("""{"password": "asdf"}""")
            }

            req.requestHandled shouldBe true
            req.response shouldHaveStatus HttpStatusCode.BadRequest
            req.response.content shouldBe "Field 'email' is required, but it was missing"
        }
    }
})
