package test.ktortemplate.authservice.service

import test.ktortemplate.authservice.model.ServicePermission

interface KeycloakEntitlementService {
    fun getEntitlements(organizationId: String, role: String): List<ServicePermission>
    fun emptyCacheForRole(organizationId: String, role: String)
}
