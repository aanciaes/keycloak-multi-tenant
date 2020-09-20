package test.ktortemplate.usermanagement.httphandler

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.common.model.ApiKeyNotFound
import test.ktortemplate.common.model.ErrorResponseEntity
import test.ktortemplate.common.utils.AuthHandlerUtils.fineGrainedAuthorization
import test.ktortemplate.usermanagement.model.GenerateApiKeyForm
import test.ktortemplate.usermanagement.service.ApiKeysService

internal class ApiKeysHandlerInjector : KoinComponent {
    val service: ApiKeysService by inject()
}

fun Route.apiKeysHandler() {

    val injector = ApiKeysHandlerInjector()
    val service = injector.service

    get("/apikey") {
        fineGrainedAuthorization(
            "user-management",
            "apikey",
            "list",
            ""
        ) { authorizedUser ->
            val apiKeys = service.getApiKeys(authorizedUser)
            call.respond(HttpStatusCode.OK, apiKeys)
        }
    }

    post("/apikey") {
        fineGrainedAuthorization(
            "user-management",
            "apikey",
            "create",
            ""
        ) { authorizedUser ->
            val registerClientForm: GenerateApiKeyForm = call.receive()
            val newCli = service.createApiKey(authorizedUser, registerClientForm)

            call.respond(HttpStatusCode.OK, newCli)
        }
    }

    delete("/apikey/{apikeyId}") {
        fineGrainedAuthorization(
            "user-management",
            "apikey",
            "delete",
            ""
        ) { authorizedUser ->
            try {
                val apiKeyId = call.parameters["apikeyId"]
                requireNotNull(apiKeyId)

                service.deleteApiKey(authorizedUser, apiKeyId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    is ApiKeyNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }
}
