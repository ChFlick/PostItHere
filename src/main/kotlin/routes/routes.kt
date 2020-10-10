package routes

import app.forms
import app.users
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
fun Routing.routes() {
    users()
    forms()

    // Only for test
    authenticate {
        get("secret") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
