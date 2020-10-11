package app

import ConfigStore
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.features.BadRequestException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.request.receiveOrNull
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.getOrFail
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
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
        authenticate {
            route("{userId}") {
                route("forms") {
                    post { addForm(userService) }
                }
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.login(userService: UserService) {
    val jwtConfig by di().instance<JwtConfig>()

    val credentials =
        when (call.request.contentType()) {
            ContentType.Application.FormUrlEncoded -> call.receiveParameters().let {
                EmailPasswordCredential(
                    it.getOrFail("email"),
                    it.getOrFail("password")
                )
            }
            ContentType.Application.Json -> call.receive()
            else -> {
                throw BadRequestException("Unsupported ContentType ${call.request.contentType()}")
            }
        }
    val user = userService.getUserByCredentials(credentials)

    if (user == null) {
        call.respond(HttpStatusCode.Unauthorized)
    } else {
        val token = jwtConfig.makeToken(user)
        call.respondText(token)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.register(userService: UserService) {
    val user = call.receive<User>()

    try {
        userService.addUser(user)
        call.respond(HttpStatusCode.Created)
    } catch (e: MongoWriteException) {
        if (e.error.category == ErrorCategory.DUPLICATE_KEY) {
            call.respond(HttpStatusCode.NotModified)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.addForm(userService: UserService) {
    val userId = checkNotNull(call.parameters["userId"])
    val form = call.receive<Form>()

    try {
        userService.addFormToUser(userId, form)
        call.respond(HttpStatusCode.Created)
    } catch (e: MongoWriteException) {
        if (e.error.category == ErrorCategory.DUPLICATE_KEY) {
            call.respond(HttpStatusCode.NotModified)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
