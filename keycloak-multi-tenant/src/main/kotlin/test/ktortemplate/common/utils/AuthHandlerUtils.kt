package test.ktortemplate.common.utils

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import org.koin.core.KoinComponent
import test.ktortemplate.authservice.model.AuthenticatedPrincipal
import test.ktortemplate.common.model.ErrorResponseEntity

object AuthHandlerUtils : KoinComponent {

    fun PipelineContext<Unit, ApplicationCall>.getPrincipalOrThrow(): AuthenticatedPrincipal {
        return call.principal() ?: throw Error("No aurora authenticated principal present")
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.fineGrainedAuthorization(
        service: String,
        parentResource: String,
        requiredScope: String,
        resourceId: String,
        func: suspend PipelineContext<Unit, ApplicationCall>.(user: AuthenticatedPrincipal) -> Unit
    ) {

        val authenticatedUser = getPrincipalOrThrow()
        val permission = authenticatedUser.role.entitlements
            .find { it.service == service }?.resourcePermissions
            ?.find { it.parentResource == parentResource || it.parentResource == "*" }?.permissions
            ?.find { it.actions.contains(requiredScope) || it.actions.contains("*") }?.resources
            ?.find { it == resourceId || it == "*" }

        if (permission != null) {
            func(authenticatedUser)
        } else {
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponseEntity("No permission for $service:$parentResource:$requiredScope:$resourceId")
            )
        }
    }
}
