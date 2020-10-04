package routes

import app.forms
import app.users
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
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
