package test.ktortemplate.authservice.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.auth.Principal
import org.keycloak.admin.client.Keycloak
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.common.utils.customDecodeBase64String

class AuthenticatedPrincipal(val userId: String, val userName: String, val organizationId: String, val role: UserRole) :
    Principal {
    companion object {

        fun fromToken(token: String): AuthenticatedPrincipal {
            val decodedToken = token.customDecodeBase64String()
            return JsonSettings.mapper.readValue(decodedToken)
        }
    }
}

data class UserRole(val name: String, val entitlements: List<ServicePermission>)

data class ServicePermission(val service: String, val resourcePermissions: List<ResourcePermission>)
data class ResourcePermission(val parentResource: String, val permissions: List<Action>)
data class Action(val actions: List<String>, val resources: List<String>)

data class KeycloakConfig(
    val serverUrl: String,
    val realm: String,
    val clientId: String,
    val secretId: String,
    val keycloak: Keycloak,
    val backendClientId: String
)
