package test.ktortemplate.common.utils

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationFunction
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import test.ktortemplate.authservice.model.AuthenticatedPrincipal

class HeaderAuthenticationProvider(configuration: Configuration) : AuthenticationProvider(configuration) {

    internal val authenticationFunction = configuration.authenticationFunction

    var headerName = configuration.headerName

    /**
     * HeaderAuthenticationProvider configuration
     */
    class Configuration internal constructor(name: String?) : AuthenticationProvider.Configuration(name) {
        internal var authenticationFunction: AuthenticationFunction<String> = {
            throw NotImplementedError(
                "HeaderAuthenticationProvider validate function is not specified. Use basic { validate { ... } } to fix."
            )
        }

        /**
         * Specifies the header where the authorizationToken is injected from the API gateway
         */
        var headerName: String = "X-Authz-Token"

        /**
         * Sets a validation function that will take the given [userIdHeader] and return [Principal],
         * or null if for some reason that user is not valid
         */
        fun validate(body: suspend ApplicationCall.(String) -> Principal?) {
            authenticationFunction = body
        }
    }
}

/**
 * Installs Header Authentication mechanism
 */
fun Authentication.Configuration.auroraHeaderAuthentication(
    name: String? = null,
    configure: (HeaderAuthenticationProvider.Configuration.() -> Unit)? = null
) {
    val configuration: HeaderAuthenticationProvider.Configuration.() -> Unit = {
        headerName = "X-Authz-Token"

        validate { authzToken -> AuthenticatedPrincipal.fromToken(authzToken) }
    }

    val provider = HeaderAuthenticationProvider(HeaderAuthenticationProvider.Configuration(name).apply(configure ?: configuration))
    val headerName = provider.headerName

    val authenticate = provider.authenticationFunction

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val principal: Principal? = try {
            val authzToken: String = call.request.header(headerName)!!

            authenticate(call, authzToken)
        } catch (e: Exception) {
            // Any exception parsing the headers will return a null principal
            null
        }

        val cause = when (principal) {
            null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        if (cause != null) {
            context.challenge(headerAuthKey, cause) {
                call.respond(HttpStatusCode.Unauthorized)
                it.complete()
            }
        }
        if (principal != null) {
            context.principal(principal)
        }
    }

    register(provider)
}

private val headerAuthKey: Any = "HeaderAuth"
