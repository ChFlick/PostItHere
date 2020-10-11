package app

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import kotlinx.serialization.ExperimentalSerializationApi
import org.kodein.di.instance
import org.kodein.di.ktor.di
import java.time.ZonedDateTime

@ExperimentalSerializationApi
fun Routing.forms() {
    val formService by di().instance<FormService>()

    route("forms") {
        route("{formId}") {

            authenticate {
                get {
                    getSubmittedForms(formService)
                }
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

@ExperimentalSerializationApi
private suspend fun PipelineContext<Unit, ApplicationCall>.getSubmittedForms(
    formService: FormService
) {
    val formId = call.parameters["formId"]

    if (formId == null) {
        call.respond(HttpStatusCode.BadRequest, "A formId must be provided in the path")
        return
    }

    val forms = formService.getSubmitsByFormId(formId)
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

    val parameterContainer =
        if (call.request.httpMethod === HttpMethod.Get) call.parameters
        else call.receiveParameters()
    val parameters = parameterContainer.toMap()
        .filterKeys { it != "formId" }
        .mapValues { it.value.single() }
    val request = FormSubmit(
        formId,
        call.request.headers["origin"],
        parameters,
        ZonedDateTime.now()
    )

    if (formService.insertRequest(request)) {
        call.respond(HttpStatusCode.Created, request)
    } else {
        call.respond(HttpStatusCode.InternalServerError)
    }
}
