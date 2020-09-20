package test.ktortemplate.authservice.service

import io.ktor.request.ApplicationRequest
import test.ktortemplate.authservice.model.AuthenticatedPrincipal

interface AuthenticationService {
    fun verify(request: ApplicationRequest): AuthenticatedPrincipal
    fun filterEntitlements(call: ApplicationRequest, principal: AuthenticatedPrincipal): AuthenticatedPrincipal
}
