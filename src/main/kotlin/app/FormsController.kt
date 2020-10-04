package app

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.kodein.di.instance
import org.kodein.di.ktor.di
import java.time.ZonedDateTime

@ExperimentalSerializationApi
fun Routing.forms() {
    val formService by di().instance<FormService>()

    authenticate {
        route("forms") {
            route("{formId}") {

                get {
                    getSubmittedForms(formService)
                }

                route("submit") {
                    get {
                        submitForm(formService)
                    }
                    post {
                        submitForm(formService)
                    }
                }
            }
        }
    }

}

@ExperimentalSerializationApi
private suspend fun PipelineContext<Unit, ApplicationCall>.getSubmittedForms(
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
private suspend fun PipelineContext<Unit, ApplicationCall>.submitForm(
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
