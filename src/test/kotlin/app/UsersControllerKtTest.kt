package app

import BooleanConfig
import com.mongodb.client.model.UpdateOptions
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setValue
import org.mindrot.jbcrypt.BCrypt
import testUser
import withServer

class UsersControllerKtTest : StringSpec({
    afterEach {
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val userColl = database.getCollection<User>("users")
            runBlocking {
                userColl.deleteMany()
            }
        }
    }

    "Login should fail with invalid password" {
        withServer {
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
        withServer {
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
        withServer {
            val database by di { application }.instance<CoroutineDatabase>()
            val userColl = database.getCollection<User>("users")
            runBlocking {
                userColl.insertOne(
                    User(
                        email = testUser.email,
                        password = BCrypt.hashpw(testUser.password, BCrypt.gensalt())
                    )
                )
            }

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
})
