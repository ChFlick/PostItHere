package app

import DatabaseStringSpec
import TestDbContainer
import addJwtHeader
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.test.TestCase
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
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
import java.time.ZoneId
import java.time.ZonedDateTime

@KtorExperimentalAPI
@ExperimentalSerializationApi
class FormsControllerKtTest : DatabaseStringSpec() {
    companion object {
        private const val EXISTING_FORM_ID = "1"
        private const val NON_EXISTING_FORM_ID = "nonexisting"
    }

    private val jwtConfig = JwtConfig(testConfig)
    private var testDi: DI = DI {}

    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)

        val client = TestDbContainer.client
        testDi = DI {
            bind<CoroutineDatabase>() with singleton { client }
            bind<UserService>() with singleton { MongoUserService(instance()) }
            bind<FormService>() with singleton { FormService(instance()) }
            bind<JwtConfig>() with singleton { jwtConfig }
        }

        val userColl = client.getCollection<User>(USERS_COLLECTION)
        runBlocking {
            userColl.insertOne(
                User(
                    testUser.id,
                    testUser.email,
                    BCrypt.hashpw(testUser.password, BCrypt.gensalt()),
                    forms = listOf(
                        Form(
                            formId = EXISTING_FORM_ID,
                            name = "someForm"
                        )
                    )
                )
            )
        }
    }

    init {
        "Fetching submitted form data should fail without auth" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/${EXISTING_FORM_ID}"
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Unauthorized
                req.response.content.shouldBeNull()
            }
        }

        "Fetching submitted form data without forms should return an empty list" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/${EXISTING_FORM_ID}"
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.OK
                req.response.content shouldBe "[]"
            }
        }

        "Fetching submitted form data for an non-existing form should return an 404" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/$NON_EXISTING_FORM_ID"
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.NotFound
                req.response.content shouldBe "The form with the formId $NON_EXISTING_FORM_ID was not found"
            }
        }

        "Fetching submitted form data with forms should return those forms" {
            withTestApplication({ run(testDi) }) {
                val testForm = FormSubmit(
                    EXISTING_FORM_ID,
                    "origin",
                    mapOf("left" to "right"),
                    ZonedDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneId.systemDefault())
                )

                runBlocking {
                    TestDbContainer.client.getCollection<FormSubmit>(SUBMITTED_FORMS_COLLECTION).insertOne(testForm)
                }

                val req = handleRequest {
                    uri = "/forms/${EXISTING_FORM_ID}"
                    addJwtHeader()
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.OK
                req.response.content shouldBe Json.encodeToString(listOf(testForm))
            }
        }

        "submitting form data via GET should work" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/${EXISTING_FORM_ID}/submit?somekey=somevalue"
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Created
                req.response.content shouldNotBe null
                val form = Json.decodeFromString<FormSubmit>(req.response.content!!)
                form.formId.length shouldBeGreaterThan 0
                form.parameters shouldContainExactly mapOf("somekey" to "somevalue")
            }
        }

        "submitting form data should fail for a non-existing form" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/$NON_EXISTING_FORM_ID/submit"
                    method = HttpMethod.Post
                    addHeader("Content-Type", ContentType.Application.FormUrlEncoded.toString())
                    setBody(FormDataContent(parametersOf("somekey", "somevalue")).formData.formUrlEncode())
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.NotFound
                req.response.content shouldBe "The form with the formId $NON_EXISTING_FORM_ID was not found"
            }
        }

        "submitting form data via POST should work" {
            withTestApplication({ run(testDi) }) {
                val req = handleRequest {
                    uri = "/forms/${EXISTING_FORM_ID}/submit"
                    method = HttpMethod.Post
                    addHeader("Content-Type", ContentType.Application.FormUrlEncoded.toString())
                    setBody(FormDataContent(parametersOf("somekey", "somevalue")).formData.formUrlEncode())
                }

                req.requestHandled shouldBe true
                req.response shouldHaveStatus HttpStatusCode.Created
                req.response.content shouldNotBe null
                val form = Json.decodeFromString<FormSubmit>(req.response.content!!)
                form.formId.length shouldBeGreaterThan 0
                form.parameters shouldContainExactly mapOf("somekey" to "somevalue")
            }
        }
    }
}
