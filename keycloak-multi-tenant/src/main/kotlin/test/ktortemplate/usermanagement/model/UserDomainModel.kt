package test.ktortemplate.usermanagement.model

import test.ktortemplate.authservice.model.ServicePermission

data class User(
    val userId: String,
    val clientId: String,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val createdAt: String,
    val roles: List<String>,
    val isEnabled: Boolean,
    val emailVerified: Boolean
)

data class UserListItem(
    val userId: String,
    val clientId: String,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val createdAt: String,
    val isEnabled: Boolean,
    val emailVerified: Boolean
)

data class InvitedUser(
    val email: String,
    val roles: List<String>
)

data class CreateUserCommand(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val temporaryCredentials: Boolean,
    val roles: List<String>
)

data class InviteUserCommand(
    val email: String,
    val roles: List<String>
)

data class Session(
    val id: String,
    val username: String,
    val userId: String,
    val ipAddress: String,
    val start: String,
    val lastAccess: String
)

data class CreateUpdateRoleCommand(
    val roleName: String,
    val roleDescription: String,
    val entitlements: List<ServicePermission>
)

data class RoleManagementCommand(
    val roles: List<String>
)

data class SetResourcePermission(
    val scopes: List<String>
)

data class PermissionByRoleListItem(val id: String, val name: String, val description: String)
data class Permission(
    val id: String,
    val name: String,
    val description: String,
    val role: String,
    val resource: String,
    val scopes: List<String>
)
