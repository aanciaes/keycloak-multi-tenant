package test.ktortemplate.authservice.model

/* ktlint-disable import-ordering */
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.KeycloakBuilder
import test.ktortemplate.common.utils.JsonSettings
import java.io.File

class KeycloakInterfaceConfigs(private val keycloakRealmsFolder: String, private val keycloakHostOverwrite: String?) {

    private val keycloakConfigs = mutableMapOf<String, KeycloakConfig>()

    fun getCorrectKeycloakConfig(organizationId: String): KeycloakConfig {
        if (!keycloakConfigs.containsKey(organizationId)) {
            val config =
                JsonSettings.mapper.readTree(File("$keycloakRealmsFolder/keycloak-$organizationId.json"))

            // Overwriting keycloak host because running inside cluster without global dns name
            val serverUrl = if (keycloakHostOverwrite != null) {
                config.get("auth-server-url").asText()!!.replace(Regex("http://(.*):8080"), keycloakHostOverwrite)
            } else {
                config.get("auth-server-url").asText()!!
            }
            val realm = config.get("realm").asText()!!
            val clientId = config.get("resource").asText()!!
            val clientSecret = config.get("credentials").get("secret").asText()!!

            val keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .resteasyClient(ResteasyClientBuilder().build())
                .build()

            val realmResource = keycloak.realm(realm)
            val clientResource = realmResource.clients().findByClientId("backend").first()!!
            keycloakConfigs[realm] =
                KeycloakConfig(
                    serverUrl,
                    realm,
                    clientId,
                    clientSecret,
                    keycloak,
                    clientResource.id
                )
            return keycloakConfigs[realm]!!
        } else {
            return keycloakConfigs[organizationId]!!
        }
    }
}
