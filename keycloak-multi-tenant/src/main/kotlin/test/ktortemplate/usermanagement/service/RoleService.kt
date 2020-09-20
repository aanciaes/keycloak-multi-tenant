package test.ktortemplate.usermanagement.service

import com.fasterxml.jackson.module.kotlin.readValue
import org.keycloak.representations.idm.RoleRepresentation
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.model.AuthenticatedPrincipal
import test.ktortemplate.authservice.model.KeycloakInterfaceConfigs
import test.ktortemplate.authservice.model.ServicePermission
import test.ktortemplate.common.model.RoleNotFound
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.usermanagement.model.Role
import test.ktortemplate.usermanagement.model.RoleListItem

class RoleService : KoinComponent {

    private val keycloakInterfaceConfigs: KeycloakInterfaceConfigs by inject()

    fun getRoles(authenticatedUser: AuthenticatedPrincipal): List<RoleListItem> {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        return keycloakConfig.keycloak.realm(keycloakConfig.realm).clients().get(keycloakConfig.backendClientId).roles()
            .list().map {
                RoleListItem(
                    it.name,
                    it.description
                )
            }
    }

    fun getRole(authenticatedUser: AuthenticatedPrincipal, role: String): Role {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        keycloakConfig.keycloak.realm(keycloakConfig.realm).clients().get(keycloakConfig.backendClientId).roles()
            .get(role).let {
                val roleRep = it.toRepresentation() ?: throw RoleNotFound(
                    "Role <$role> does not exist"
                )
                val entitlementsAttribute = roleRep.attributes["entitlements"]?.first()
                return if (entitlementsAttribute == null) {
                    Role(
                        roleRep.name,
                        roleRep.description,
                        emptyList()
                    )
                } else {
                    val entitlements =
                        JsonSettings.mapper.readValue<ArrayList<ServicePermission>>(entitlementsAttribute).toList()
                    Role(
                        roleRep.name,
                        roleRep.description,
                        entitlements
                    )
                }
            }
    }

    fun createRole(authenticatedUser: AuthenticatedPrincipal, roleName: String, roleDescription: String, entitlements: List<ServicePermission>): Role? {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val rolesResources = realmResource.clients().get(keycloakConfig.backendClientId).roles()

        val roleRepresentation = RoleRepresentation()
        roleRepresentation.name = roleName
        roleRepresentation.description = roleDescription
        roleRepresentation.attributes = mutableMapOf(Pair("entitlements", listOf(JsonSettings.minMapper.writeValueAsString(entitlements))))

        rolesResources.create(roleRepresentation)
        rolesResources.get(roleName).update(roleRepresentation)

        return Role(roleName, roleDescription, entitlements)
    }

    fun updateRole(authenticatedUser: AuthenticatedPrincipal, roleName: String, roleDescription: String, entitlements: List<ServicePermission>): Role? {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val rolesResources = realmResource.clients().get(keycloakConfig.backendClientId).roles()

        val roleResource = rolesResources.get(roleName)
        val roleRepresentation = roleResource.toRepresentation()
        roleRepresentation.name = roleName
        roleRepresentation.description = roleDescription
        roleRepresentation.attributes = mutableMapOf(Pair("entitlements", listOf(JsonSettings.minMapper.writeValueAsString(entitlements))))

        roleResource.update(roleRepresentation)

        return Role(roleName, roleDescription, entitlements)
    }

    fun deleteRole(authenticatedUser: AuthenticatedPrincipal, roleName: String) {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val rolesResources = realmResource.clients().get(keycloakConfig.backendClientId).roles()

        rolesResources.deleteRole(roleName)
    }
}
