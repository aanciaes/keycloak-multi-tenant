package test.ktortemplate.authservice.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.ktor.request.ApplicationRequest
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.model.AuthenticatedPrincipal
import test.ktortemplate.authservice.model.UserRole
import test.ktortemplate.common.model.AuthenticationFailed
import test.ktortemplate.common.utils.JsonSettings

class AuthenticationServiceImpl : AuthenticationService, KoinComponent {

    private val signingKeyResolver: SigningKeyResolverAdapter by inject()
    private val entitlementService: KeycloakEntitlementService by inject()

    override fun verify(request: ApplicationRequest): AuthenticatedPrincipal {
        return try {
            val token = getTokenFromRequest(request)

            // This fetches new signing key if not exists already and verifies JWT
            val claims = Jwts.parser()
                .setSigningKeyResolver(signingKeyResolver)
                .parseClaimsJws(token)

            val realm = getRealmFromToken(claims)
            val role = getRoleFromToken(claims)
            val entitlements = entitlementService.getEntitlements(realm, role)

            AuthenticatedPrincipal(
                claims.body.subject,
                claims.body.get("preferred_username", String::class.java)!!,
                realm,
                UserRole(
                    role,
                    entitlements
                )
            )
        } catch (e: Exception) {
            throw AuthenticationFailed(e.message ?: e.toString())
        }
    }

    // TODO: Filter entitlements based on request to minimize the size of authz token passed to services
    override fun filterEntitlements(
        call: ApplicationRequest,
        principal: AuthenticatedPrincipal
    ): AuthenticatedPrincipal {
        return principal
    }

    private fun getTokenFromRequest(request: ApplicationRequest): String {
        val authorizationHeader = request.headers["Authorization"] ?: throw Exception("No authorization token")
        return authorizationHeader.split(" ").getOrNull(1) ?: throw Exception("No authorization bearer")
    }

    private fun getRealmFromToken(claims: Jws<Claims>): String {
        val issuer = claims.body.issuer ?: throw Exception("No issuer present in token")
        return issuer.substring(issuer.indexOf("realms/")).split("/")[1]
    }

    private fun getRoleFromToken(claims: Jws<Claims>): String {
        val resourceAccessString =
            JsonSettings.mapper.writeValueAsString(claims.body.get("resource_access", LinkedHashMap::class.java))
        val resourceAccess = JsonSettings.mapper.readTree(resourceAccessString)
        val backendAccess = resourceAccess["backend"]

        return backendAccess.get("roles").get(0).asText()
    }
}
