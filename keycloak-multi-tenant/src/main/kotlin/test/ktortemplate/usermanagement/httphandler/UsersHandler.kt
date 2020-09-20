package test.ktortemplate.usermanagement.httphandler

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import org.koin.core.KoinComponent
import org.koin.core.inject
import test.ktortemplate.common.model.EmailNotSentError
import test.ktortemplate.common.model.ErrorResponseEntity
import test.ktortemplate.common.model.RoleNotFound
import test.ktortemplate.common.model.SessionNotFound
import test.ktortemplate.common.model.UserNotFound
import test.ktortemplate.common.model.UsernameAlreadyUsed
import test.ktortemplate.common.utils.AuthHandlerUtils.fineGrainedAuthorization
import test.ktortemplate.usermanagement.model.CreateUserCommand
import test.ktortemplate.usermanagement.model.InviteUserCommand
import test.ktortemplate.usermanagement.model.RoleManagementCommand
import test.ktortemplate.usermanagement.service.UserService

internal class UsersHandlerInjector : KoinComponent {
    val userService: UserService by inject()
}

fun Route.usersHandler() {

    val injector = UsersHandlerInjector()
    val userService = injector.userService

    // checked
    get("/user") {
        fineGrainedAuthorization(
            "user-management",
            "user",
            "list",
            ""
        ) { authorizedUser ->

            val username = call.request.queryParameters["username"]
            val email = call.request.queryParameters["email"]
            val firstName = call.request.queryParameters["firstName"]
            val lastName = call.request.queryParameters["lastName"]
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            val offset = call.request.queryParameters["offset"]?.toInt() ?: 0

            val users = userService.getUsers(authorizedUser, username, email, firstName, lastName, limit, offset)
            call.respond(HttpStatusCode.OK, users)
        }
    }

    get("/user/{id}") {
        val id = call.parameters["id"]
        requireNotNull(id)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "view",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.getUser(authorizedUser, id)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    post("/user") {
        val newUser: CreateUserCommand = call.receive()

        fineGrainedAuthorization(
            "user-management",
            "user",
            "create",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.createUser(authorizedUser, newUser)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is RoleNotFound -> call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponseEntity(e.message)
                    )
                    is UsernameAlreadyUsed -> call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    post("/user/invite") {
        val newUser: InviteUserCommand = call.receive()

        fineGrainedAuthorization(
            "user-management",
            "user",
            "create",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.inviteUser(authorizedUser, newUser)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is RoleNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    put("/user/{userId}/role") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)
        val assignRolesCommand: RoleManagementCommand = call.receive()

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.assignRole(authorizedUser, userId, assignRolesCommand.roles)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is RoleNotFound, is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    delete("/user/{userId}/role") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)
        val deleteRolesCommand: RoleManagementCommand = call.receive()

        fineGrainedAuthorization(
            "user-management",
            "user",
            "delete",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.unAssignRole(authorizedUser, userId, deleteRolesCommand.roles)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is RoleNotFound, is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    // As email
    put("/user/{userId}/otp") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                userService.configureOTP(authorizedUser, userId, false)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    is EmailNotSentError -> call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    // Required on login
    put("/user/{userId}/force-otp") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                userService.configureOTP(authorizedUser, userId, true)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    // As email
    put("/user/{userId}/resetpwd") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                userService.resetPwdEmail(authorizedUser, userId, false)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    // Required on login
    put("/user/{userId}/force-resetpwd") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                userService.resetPwdEmail(authorizedUser, userId, true)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    put("/user/{userId}/disable") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.disableUser(authorizedUser, userId)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    put("/user/{userId}/enable") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "edit",
            ""
        ) { authorizedUser ->
            try {
                val user = userService.enableUser(authorizedUser, userId)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    get("/user/{userId}/session") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "view",
            ""
        ) { authorizedUser ->
            try {
                val sessions = userService.getUserSessions(authorizedUser, userId)
                call.respond(HttpStatusCode.OK, sessions)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    delete("/user/{userId}/session/{sessionId}") {
        val sessionId = call.parameters["sessionId"]
        requireNotNull(sessionId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "delete",
            ""
        ) { authorizedUser ->
            try {
                userService.deleteUserSession(authorizedUser, sessionId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound, is SessionNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }

    delete("/user/{userId}/session") {
        val userId = call.parameters["userId"]
        requireNotNull(userId)

        fineGrainedAuthorization(
            "user-management",
            "user",
            "delete",
            ""
        ) { authorizedUser ->
            try {
                userService.deleteAllUserSessions(authorizedUser, userId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                when (e) {
                    // log.error(e)
                    is UserNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponseEntity(e.message)
                    )
                    else -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseEntity("An error occurred")
                    )
                }
            }
        }
    }
}
