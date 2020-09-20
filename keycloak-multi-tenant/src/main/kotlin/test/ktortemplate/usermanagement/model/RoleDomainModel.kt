package test.ktortemplate.usermanagement.model

import test.ktortemplate.authservice.model.ServicePermission

data class RoleListItem(val name: String, val description: String)
data class Role(val name: String, val description: String, val entitlements: List<ServicePermission>)
