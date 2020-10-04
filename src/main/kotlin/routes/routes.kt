package routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.kodein.di.instance
import org.kodein.di.ktor.di
import security.JwtConfig
import service.*
import java.time.ZonedDateTime

@ExperimentalSerializationApi
fun Routing.routes() {
    route("/users") {
        val userService by di().instance<UserService>()

        post("login") {
            val credentials = call.receive<EmailPasswordCredential>()
            val user = userService.getUserByCredentials(credentials)

            if(user == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val token = JwtConfig.makeToken(user)
                call.respondText(token)
            }
        }

        post("/register") {
            val user = call.receiveOrNull(User::class)

            if (user != null && userService.addUser(user)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    authenticate {
        route("/postit/{formId}") {
            val formService by di().instance<FormService>()

            get {
                getForms(formService)
            }

            route("/submit") {
                get {
                    formSubmit(formService)
                }
                post {
                    formSubmit(formService)
                }
            }
        }
    }
}

@ExperimentalSerializationApi
private suspend fun PipelineContext<Unit, ApplicationCall>.getForms(
    formService: FormService
) {
    val formId = call.parameters["formId"]

    if (formId == null) {
        call.respond(HttpStatusCode.BadRequest, "A formId must be provided in the path")
        return
    }

    val forms = formService.getFormsByFormId(formId)
    call.respond(forms)
}

@ExperimentalSerializationApi
private suspend fun PipelineContext<Unit, ApplicationCall>.formSubmit(
    formService: FormService
) {
    val formId = call.parameters["formId"]

    if (formId == null) {
        call.respond(HttpStatusCode.BadRequest, "A formId must be provided in the path")
        return
    }

    val parameters = call.parameters.toMap()
        .filterKeys { it != "formId" }
        .mapValues { it.value.single() }
    val request = Form(
        formId,
        call.request.headers["origin"],
        parameters,
        ZonedDateTime.now()
    )

    if (formService.insertRequest(request)) {
        call.respond("Ok")
    } else {
        call.respond(HttpStatusCode.InternalServerError)
    }
}