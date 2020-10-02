package routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import service.Form
import service.FormService
import java.time.ZonedDateTime

fun Routing.routes() {
    route("/postit/{formId}") {
        val formService by di().instance<FormService>()

        get {
            val formId = call.parameters["formId"]

            if (formId == null) {
                call.respond(HttpStatusCode.BadRequest, "A formId must be provided in the path")
                return@get
            }

            val forms = formService.getFormsByFormId(formId)
            call.respond(forms)
        }

        route("/submit") {

            get {
                val formId = call.parameters["formId"]

                if (formId == null) {
                    call.respond(HttpStatusCode.BadRequest, "A formId must be provided in the path")
                    return@get
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
            post {
            }
        }
    }
}