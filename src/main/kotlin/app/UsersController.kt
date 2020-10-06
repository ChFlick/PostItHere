package app

import ConfigStore
import com.configcat.ConfigCatClient
import com.mongodb.DuplicateKeyException
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
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
    val config by di().instance<ConfigStore>()

    route("users") {
        post("login") { login(userService) }
        route("register") {
            intercept(ApplicationCallPipeline.Features) {
                config.getValue("allowRegistration", false)
                    .takeIf { !it }
                    ?.apply {
                        call.respond(HttpStatusCode.ServiceUnavailable)
                        finish()
                    }
            }

            post { register(userService) }
        }
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

    if (user != null) {
        try {
            userService.addUser(user)
            call.respond(HttpStatusCode.Created)
        } catch (e: MongoWriteException) {
            if (e.error.category.equals(ErrorCategory.DUPLICATE_KEY)) {
                call.respond(HttpStatusCode.NotModified)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    } else {
        call.respond(HttpStatusCode.InternalServerError)
    }
}
