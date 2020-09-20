package test.ktortemplate.usermanagement.service

/* ktlint-disable import-ordering */
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.authservice.model.AuthenticatedPrincipal
import test.ktortemplate.common.model.EmailNotSentError
import test.ktortemplate.authservice.model.KeycloakConfig
import test.ktortemplate.authservice.model.KeycloakInterfaceConfigs
import test.ktortemplate.common.model.RoleNotFound
import test.ktortemplate.common.model.SessionNotFound
import test.ktortemplate.common.model.UserNotFound
import test.ktortemplate.common.model.UsernameAlreadyUsed
import test.ktortemplate.usermanagement.model.CreateUserCommand
import test.ktortemplate.usermanagement.model.InviteUserCommand
import test.ktortemplate.usermanagement.model.InvitedUser
import test.ktortemplate.usermanagement.model.Session
import test.ktortemplate.usermanagement.model.User
import test.ktortemplate.usermanagement.model.UserListItem
import java.util.Random
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

class UserService : KoinComponent {

    private val keycloakInterfaceConfigs: KeycloakInterfaceConfigs by inject()

    fun getUsers(
        authenticatedUser: AuthenticatedPrincipal,
        userName: String?,
        email: String?,
        firstName: String?,
        lastName: String?,
        limit: Int,
        offset: Int
    ): List<UserListItem> {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)

        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val users = realmResource.users().search(userName, firstName, lastName, email, offset, limit, true)

        println(keycloakConfig.keycloak.tokenManager().accessToken.token)
        // There seams to be a bug in keycloak where when filtering by username returns the service accounts
        return users.filter { !it.username.contains("service-account") }
            .map { it.toListItem(authenticatedUser.organizationId) }
    }

    // checked
    fun getUser(authenticatedUser: AuthenticatedPrincipal, id: String): User {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)

            val userResource = realmResource.users().get(id)
            val roles = userResource.roles().clientLevel(keycloakConfig.backendClientId).listAll().map { it.name }
            return userResource.toRepresentation().toCustomRepresentation(authenticatedUser.organizationId, roles)
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$id> does not exist")
        }
    }

    // checked
    // TODO: Maybe place role in attributes to have it when listing users
    fun createUser(authenticatedUser: AuthenticatedPrincipal, createUserCommand: CreateUserCommand): User {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)

        val userRepresentation = UserRepresentation()
        userRepresentation.username = createUserCommand.username
        userRepresentation.firstName = createUserCommand.firstName
        userRepresentation.lastName = createUserCommand.lastName
        userRepresentation.email = createUserCommand.email
        userRepresentation.isEnabled = true

        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val usersResource = realmResource.users()

        val passwordCred = CredentialRepresentation()
        passwordCred.isTemporary = createUserCommand.temporaryCredentials
        passwordCred.type = CredentialRepresentation.PASSWORD
        passwordCred.value = createUserCommand.password

        val rolesToApply = checkExitingRoles(keycloakConfig, createUserCommand.roles)

        // Create User
        val response = realmResource.users().create(userRepresentation)
        if (response.status == 409) throw UsernameAlreadyUsed("Username <${createUserCommand.username}> already in use")
        val userId = CreatedResponseUtil.getCreatedId(response)
        val userResource = usersResource.get(userId)

        // Set password credential
        userResource.resetPassword(passwordCred)

        // Assign client level role to user
        userResource.roles().clientLevel(keycloakConfig.backendClientId).add(rolesToApply)

        return userResource.toRepresentation()
            .toCustomRepresentation(authenticatedUser.organizationId, rolesToApply.map { it.name })
    }

    // Checked
    fun inviteUser(authenticatedUser: AuthenticatedPrincipal, invitedUser: InviteUserCommand): InvitedUser {
        val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)

        val tempUserName = "${invitedUser.email.split("@").first()}${(0..100).random()}"
        val userRepresentation = UserRepresentation()
        userRepresentation.username = tempUserName
        userRepresentation.email = invitedUser.email
        userRepresentation.isEnabled = true
        userRepresentation.requiredActions = listOf("UPDATE_PASSWORD", "UPDATE_PROFILE")

        val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
        val usersResource = realmResource.users()

        val rolesToApply = checkExitingRoles(keycloakConfig, invitedUser.roles)

        // Create user
        val response = realmResource.users().create(userRepresentation)
        val userId = CreatedResponseUtil.getCreatedId(response)
        val userResource = usersResource.get(userId)

        // Assign client level role to user
        userResource.roles().clientLevel(keycloakConfig.backendClientId).add(rolesToApply)

        // Send verify email to user which will trigger password and profile update
        // userResource.executeActionsEmail("frontend", "http://waterdog.mikesmacbookpro.local:3006/", listOf("VERIFY_EMAIL"))
        userResource.executeActionsEmail(listOf("VERIFY_EMAIL"))
        val userRep = userResource.toRepresentation()

        return InvitedUser(userRep.email, rolesToApply.map { it.name })
    }

    // Checked
    fun assignRole(authenticatedUser: AuthenticatedPrincipal, targetUser: String, targetRoles: List<String>): User {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()

            val rolesToApply = checkExitingRoles(keycloakConfig, targetRoles)

            val userResource = usersResource.get(targetUser)
            // Assign client level role to user
            val currentRoles = userResource.roles().clientLevel(keycloakConfig.backendClientId).listAll()
            userResource.roles().clientLevel(keycloakConfig.backendClientId).add(rolesToApply)
            currentRoles.addAll(rolesToApply)
            return userResource.toRepresentation()
                .toCustomRepresentation(authenticatedUser.organizationId, currentRoles.map { it.name })
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$targetUser> does not exist")
        }
    }

    // Checked
    fun unAssignRole(authenticatedUser: AuthenticatedPrincipal, targetUser: String, targetRoles: List<String>): User {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()

            val userResource = usersResource.get(targetUser)
            val rolesToDelete = checkExitingRoles(keycloakConfig, targetRoles)

            // Assign client level role to user
            val rolesResources = userResource.roles().clientLevel(keycloakConfig.backendClientId)
            val currentRoles = rolesResources.listAll().map { it.name }.toMutableList()

            rolesResources.remove(rolesToDelete)
            currentRoles.removeAll(rolesToDelete.map { it.name })

            return userResource.toRepresentation().toCustomRepresentation(authenticatedUser.organizationId, currentRoles)
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$targetUser> does not exist")
        }
    }

    // Checked
    fun configureOTP(authenticatedUser: AuthenticatedPrincipal, targetUserId: String, force: Boolean) {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()
            val userResource = usersResource.get(targetUserId)

            if (force) {
                userResource.logout() // logout for all sessions to force otp config on login
                val userRep = userResource.toRepresentation()
                userRep.requiredActions = listOf("CONFIGURE_TOTP")
                userResource.update(userRep)
            } else {
                userResource.executeActionsEmail(listOf("CONFIGURE_TOTP"))
            }
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$targetUserId> does not exist")
        } catch (e: BadRequestException) {
            throw EmailNotSentError("Email could not be sent")
        }
    }

    // Checked
    fun resetPwdEmail(authenticatedUser: AuthenticatedPrincipal, targetUserId: String, force: Boolean) {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()
            val userResource = usersResource.get(targetUserId)

            if (force) {
                userResource.logout() // logout for all sessions to force password reset config on login
                val userRep = userResource.toRepresentation()
                userRep.requiredActions = listOf("UPDATE_PASSWORD")
                userResource.update(userRep)
            } else {
                userResource.executeActionsEmail(listOf("UPDATE_PASSWORD"))
            }
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$targetUserId> does not exist")
        } catch (e: BadRequestException) {
            throw EmailNotSentError("Email could not be sent")
        }
    }

    // Checked
    fun disableUser(authenticatedUser: AuthenticatedPrincipal, userId: String): UserListItem {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()
            val userResource = usersResource.get(userId)
            val userRep = userResource.toRepresentation()

            requireNotNull(userRep)

            userResource.logout()
            userRep.isEnabled = false
            userResource.update(userRep)

            return userRep.toListItem(authenticatedUser.organizationId)
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$userId> does not exist")
        }
    }

    // Checked
    fun enableUser(authenticatedUser: AuthenticatedPrincipal, userId: String): UserListItem {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()
            val userResource = usersResource.get(userId)
            val userRep = userResource.toRepresentation()

            requireNotNull(userRep)

            userRep.isEnabled = true
            userResource.update(userRep)

            return userRep.toListItem(authenticatedUser.organizationId)
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$userId> does not exist")
        }
    }

    // Checked
    fun getUserSessions(authenticatedUser: AuthenticatedPrincipal, userId: String): List<Session> {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            val usersResource = realmResource.users()
            val userSessions = usersResource.get(userId).userSessions

            return userSessions.map {
                Session(
                    it.id,
                    it.username,
                    it.userId,
                    it.ipAddress,
                    it.start.toString(),
                    it.lastAccess.toString()
                )
            }
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$userId> does not exist")
        }
    }

    // Checked
    fun deleteUserSession(authenticatedUser: AuthenticatedPrincipal, sessionId: String) {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            realmResource.deleteSession(sessionId)
        } catch (e: NotFoundException) {
            throw SessionNotFound("Session <$sessionId> does not exist")
        }
    }

    // Checked
// Force user logout
    fun deleteAllUserSessions(authenticatedUser: AuthenticatedPrincipal, userId: String) {
        try {
            val keycloakConfig = keycloakInterfaceConfigs.getCorrectKeycloakConfig(authenticatedUser.organizationId)
            val realmResource = keycloakConfig.keycloak.realm(keycloakConfig.realm)
            realmResource.users().get(userId).logout()
        } catch (e: NotFoundException) {
            throw UserNotFound("User <$userId> does not exist")
        }
    }

    private fun UserRepresentation.toCustomRepresentation(realm: String, roles: List<String> = emptyList()): User =
        User(
            this.id,
            realm,
            this.username,
            this.email,
            this.firstName,
            this.lastName,
            this.createdTimestamp.toString(),
            roles,
            this.isEnabled,
            this.isEmailVerified
        )

    private fun UserRepresentation.toListItem(realm: String): UserListItem = UserListItem(
        this.id,
        realm,
        this.username,
        this.email,
        this.firstName,
        this.lastName,
        this.createdTimestamp.toString(),
        this.isEnabled,
        this.isEmailVerified
    )

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

    fun IntRange.random() =
        Random().nextInt((endInclusive + 1) - start) + start
}
