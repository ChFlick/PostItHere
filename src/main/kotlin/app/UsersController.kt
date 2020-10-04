package app

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import security.JwtConfig

fun Routing.users() {
    val userService by di().instance<UserService>()

    route("users") {
        post("login") { login(userService) }
        post("register") { register(userService) }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.login(userService: UserService) {
    val jwtConfig by di().instance<JwtConfig>()

    val credentials = call.receive<EmailPasswordCredential>()
    val user = userService.getUserByCredentials(credentials)

    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized)
    } else {
        val token = jwtConfig.makeToken(user)
        call.respondText(token)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.register(userService: UserService) {
    val user = call.receiveOrNull(User::class)

    if (user != null && userService.addUser(user)) {
        call.respond(HttpStatusCode.OK)
    } else {
        call.respond(HttpStatusCode.InternalServerError)
    }
}
