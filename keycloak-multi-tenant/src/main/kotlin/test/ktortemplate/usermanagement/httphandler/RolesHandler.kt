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
import test.ktortemplate.common.utils.AuthHandlerUtils.fineGrainedAuthorization
import test.ktortemplate.usermanagement.model.CreateUpdateRoleCommand
import test.ktortemplate.usermanagement.service.RoleService

internal class RolesHandlerInjector : KoinComponent {
    val roleService: RoleService by inject()
}

fun Route.rolesHandler() {

    val injector = RolesHandlerInjector()
    val roleService = injector.roleService

    get("/role") {
        fineGrainedAuthorization(
            "user-management",
            "role",
            "list",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, roleService.getRoles(authorizedUser))
        }
    }

    get("/role/{role}") {
        fineGrainedAuthorization(
            "user-management",
            "role",
            "view",
            ""
        ) { authorizedUser ->
            val role = call.parameters["role"]
            requireNotNull(role)
            call.respond(HttpStatusCode.OK, roleService.getRole(authorizedUser, role))
        }
    }

    post("/role") {
        fineGrainedAuthorization(
            "user-management",
            "role",
            "create",
            ""
        ) { authorizedUser ->
            val createRoleCommand: CreateUpdateRoleCommand = call.receive()

            when (val role =
                roleService.createRole(
                    authorizedUser,
                    createRoleCommand.roleName,
                    createRoleCommand.roleDescription,
                    createRoleCommand.entitlements
                )) {
                null -> call.respond(HttpStatusCode.BadRequest)
                else -> call.respond(HttpStatusCode.OK, role)
            }
        }
    }

    put("/role/{role}") {
        fineGrainedAuthorization(
            "user-management",
            "role",
            "edit",
            ""
        ) { authorizedUser ->
            val createRoleCommand: CreateUpdateRoleCommand = call.receive()
            val role = call.parameters["role"]
            requireNotNull(role)

            when (val role =
                roleService.updateRole(
                    authorizedUser,
                    role,
                    createRoleCommand.roleDescription,
                    createRoleCommand.entitlements
                )) {
                null -> call.respond(HttpStatusCode.BadRequest)
                else -> call.respond(HttpStatusCode.OK, role)
            }
        }
    }

    delete("/role/{role}") {
        fineGrainedAuthorization(
            "user-management",
            "role",
            "delete",
            ""
        ) { authenticatedUser ->

            val role = call.parameters["role"]
            requireNotNull(role)

            roleService.deleteRole(authenticatedUser, role)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
