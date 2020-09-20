package test.ktortemplate.authservice.service

import com.fasterxml.jackson.module.kotlin.readValue
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.model.KeycloakInterfaceConfigs
import test.ktortemplate.authservice.model.ServicePermission
import test.ktortemplate.common.utils.JsonSettings

class LocalCacheEntitlementService : KeycloakEntitlementService, KoinComponent {

    private val keycloakInterfaceConfigs: KeycloakInterfaceConfigs by inject()
    private val entitlementCache = mutableMapOf<String, List<ServicePermission>>()

    override fun getEntitlements(
        organizationId: String,
        role: String
    ): List<ServicePermission> {
        if (entitlementCache.containsKey("$organizationId:$role")) {
            return entitlementCache["$organizationId:$role"]!!
        } else {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(organizationId)
            val keycloakRole =
                keycloakConfig.keycloak.realm(keycloakConfig.realm).clients().get(keycloakConfig.backendClientId)
                    .roles().get(role).toRepresentation()

            val entitlementsAttribute = keycloakRole.attributes["entitlements"]?.first()
            requireNotNull(entitlementsAttribute)

            val entitlements =
                JsonSettings.mapper.readValue<ArrayList<ServicePermission>>(entitlementsAttribute).toList()

            entitlementCache["$organizationId:$role"] = entitlements
            return entitlements
        }
    }

    override fun emptyCacheForRole(organizationId: String, role: String) {
        entitlementCache.remove("$organizationId:$role")
    }
}
