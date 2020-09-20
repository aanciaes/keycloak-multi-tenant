package test.ktortemplate.authservice.httphandler

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.head
import io.ktor.routing.options
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.service.AuthenticationService
import test.ktortemplate.common.model.ErrorResponseEntity
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.common.utils.customEncodeToBase64String

const val prefix = "/validate"

const val xAuthZToken = "X-Authz-Token"

class AuthServiceHandlerInjector : KoinComponent {
    val authenticationService: AuthenticationService by inject()
}

fun Route.authServiceRoutes() {

    val authServiceInjector = AuthServiceHandlerInjector()
    val authenticationService = authServiceInjector.authenticationService

    // automatically validate - Test if we really need to automatically validate options requests
    options("/validate/{...}") { call.respond(HttpStatusCode.OK) }

    // Validate jwt
    get("/validate/{...}") { validate(call, authenticationService) }
    post("/validate/{...}") { validate(call, authenticationService) }
    put("/validate/{...}") { validate(call, authenticationService) }
    delete("/validate/{...}") { validate(call, authenticationService) }
    head("/validate/{...}") { validate(call, authenticationService) }
    patch("/validate/{...}") { validate(call, authenticationService) }
}

private suspend fun validate(call: ApplicationCall, authenticationService: AuthenticationService) {
    call.application.log.info("Validating request: ${call.request.uri.removePrefix(prefix)}")

    try {
        val authenticatedUser = authenticationService.verify(call.request)

        call.response.headers.append(
            xAuthZToken,
            JsonSettings.mapper.writeValueAsString(authenticatedUser).customEncodeToBase64String()
        )

        call.respond(HttpStatusCode.OK)
    } catch (e: Exception) {
        e.printStackTrace()
        call.respond(HttpStatusCode.Unauthorized, ErrorResponseEntity(e.message))
    }
}
