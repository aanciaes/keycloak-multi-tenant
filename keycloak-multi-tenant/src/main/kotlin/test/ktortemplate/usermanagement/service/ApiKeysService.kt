package test.ktortemplate.usermanagement.service

/* ktlint-disable import-ordering */
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.model.AuthenticatedPrincipal
import test.ktortemplate.common.model.ApiKeyNotFound
import test.ktortemplate.authservice.model.KeycloakConfig
import test.ktortemplate.authservice.model.KeycloakInterfaceConfigs
import test.ktortemplate.common.model.RoleNotFound
import test.ktortemplate.usermanagement.model.ApiKeyDefinition
import test.ktortemplate.usermanagement.model.ApiKeyOutput
import test.ktortemplate.usermanagement.model.GenerateApiKeyForm
import java.util.UUID
import javax.ws.rs.NotFoundException

class ApiKeysService : KoinComponent {

    private val keycloakInterfaceConfigs: KeycloakInterfaceConfigs by inject()

    fun getApiKeys(authenticatedUser: AuthenticatedPrincipal): List<ApiKeyDefinition> {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val clients = realmResource.clients().findAll(true).filter { it?.attributes?.get("apikey") == "true" }.toList()

        return clients.map { it.toCustomRepresentation() }
    }

    fun createApiKey(authenticatedUser: AuthenticatedPrincipal, registerClientForm: GenerateApiKeyForm): ApiKeyOutput {

        // Setup new client
        val client = ClientRepresentation()
        val apiKeyId = registerClientForm.clientName + "-" + UUID.randomUUID().toString()

        client.clientId = apiKeyId
        client.name = registerClientForm.clientName
        client.description = registerClientForm.description

        val clientSecret = UUID.randomUUID().toString()
        client.secret = clientSecret

        client.isServiceAccountsEnabled = true
        client.isStandardFlowEnabled = false

        client.isEnabled = true
        client.isBearerOnly = false
        client.isPublicClient = false
        client.isConsentRequired = false
        client.isImplicitFlowEnabled = false

        client.attributes = mapOf(
            Pair("access.token.lifespan", registerClientForm.ttl),
            Pair("role", registerClientForm.role),
            Pair("apikey", "true")
        )
        //

        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
        // Get realm
        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val res = realmResource.clients().create(client)
        val newClientId = CreatedResponseUtil.getCreatedId(res)

        // Fetch the user associated with new client service account
        val clientResource = realmResource.clients().get(newClientId)
        val userServiceAccount = clientResource.serviceAccountUser
        val rolesToApply = checkExitingRoles(keycloakConfig, listOf(registerClientForm.role))

        // Add role to service account user
        realmResource.users().get(userServiceAccount.id).roles()
            .clientLevel(keycloakConfig.backendClientId)
            .add(rolesToApply)

        val clientRep = clientResource.toRepresentation()
        return ApiKeyOutput(clientRep.clientId, clientSecret)
    }

    fun deleteApiKey(authenticatedUser: AuthenticatedPrincipal, apiKeyId: String) {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)

            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val apiKeyClient = realmResource.clients().get(apiKeyId)

            apiKeyClient.remove()
        } catch (e: NotFoundException) {
            throw ApiKeyNotFound("Api Key <$apiKeyId> does not exist")
        }
    }

    private fun ClientRepresentation.toCustomRepresentation(): ApiKeyDefinition {
        return ApiKeyDefinition(
            this.id,
            this.clientId,
            this.description,
            this.attributes?.get("role")
        )
    }

    private fun checkExitingRoles(
        keycloakConfig: KeycloakConfig,
        rolesToCheck: List<String>
    ): List<RoleRepresentation> {
        val roles =
            keycloakConfig.keycloak.realm(keycloakConfig.realm).clients().get(keycloakConfig.backendClientId).roles()
                .list()
        val rolesToApply = mutableListOf<RoleRepresentation>()
        rolesToCheck.forEach { role ->
            val roleRep = roles.find { it.name == role }
            if (roleRep != null) {
                rolesToApply.add(roleRep)
            } else {
                throw RoleNotFound("Role <$role> does not exist")
            }
        }

        return rolesToApply
    }
}
