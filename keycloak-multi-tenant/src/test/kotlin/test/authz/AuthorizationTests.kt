package test.authz

import io.ktor.auth.Principal
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.common.utils.customEncodeToBase64String
import testApp
import java.util.UUID

class AuthorizationTests {

    @Nested
    inner class TestingWithStaticPermissions {

        @Test
        fun `A request with no AuthZ token is not permitted`(): Unit = testApp {
            with(handleRequest(HttpMethod.Get, "/satellite")) {
                response.status() `should equal` HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `An admin user can perform all endpoints`(): Unit = testApp {

            val adminEntitlements = this.javaClass.getResource("/admin-entitlements.json").readText()
            val authZToken = adminEntitlements.customEncodeToBase64String()

            with(handleRequest(HttpMethod.Get, "/satellite") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }
        }

        @Test
        fun `An user can call endpoints if it has the required permissions`(): Unit = testApp {

            val userEntitlements = this.javaClass.getResource("/readonly-user-entilements.json").readText()
            val authZToken = userEntitlements.customEncodeToBase64String()

            // User can list satellites
            with(handleRequest(HttpMethod.Get, "/satellite") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User cannot create satellites
            with(handleRequest(HttpMethod.Post, "/satellite") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }

            // User can list system of satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User cannot create system on satellite 1
            with(handleRequest(HttpMethod.Post, "/satellite/1/system") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }

            // User can view system 1 of satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User cannot edit system 1 of satellite 1
            with(handleRequest(HttpMethod.Put, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }
        }
    }

    @Nested
    inner class TestingWithDynamicallyGeneratedPermissions {

        @Test
        fun `A request with no AuthZ token is not permitted`(): Unit = testApp {
            with(handleRequest(HttpMethod.Get, "/satellite")) {
                response.status() `should equal` HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `An admin user can perform all endpoints`(): Unit = testApp {

            val adminPrincipal = creatAuthenticatedUser(roleName = "Administrator")
            adminPrincipal.addPermissionSatManagementPermission(
                "*",
                mutableListOf("*"),
                mutableListOf("*")
            )

            // println (JsonSettings.mapper.writeValueAsString(adminPrincipal))
            val authZToken = adminPrincipal.generateAuthZToken()

            with(handleRequest(HttpMethod.Get, "/satellite") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            with(handleRequest(HttpMethod.Post, "/satellite") {
                addHeader("X-Authz-Token", authZToken)
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }
        }

        @Test
        fun `An user can list satellites only if it contains listSatellites scopes`(): Unit = testApp {

            val user = creatAuthenticatedUser(roleName = "BasicUser")

            // User without required token
            with(handleRequest(HttpMethod.Get, "/satellite") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }

            // Add required permission to user and try again
            user.addPermissionSatManagementPermission("satellite", mutableListOf("listSatellite"), mutableListOf("*"))
            with(handleRequest(HttpMethod.Get, "/satellite") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }
        }

        @Test
        fun `An user can get satellite details for permitted satellites`(): Unit = testApp {
            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite with id 1.
            user.addPermissionSatManagementPermission("satellite:1", mutableListOf("viewSatellite"), mutableListOf("*"))

            // User without required token
            with(handleRequest(HttpMethod.Get, "/satellite/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            with(handleRequest(HttpMethod.Get, "/satellite/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }
        }

        // @Test
        // TODO: Failing Test - Not implemented yet, but should be implemented
        fun `Specific parent resources permissions work with wildcard`(): Unit = testApp {

            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite all satellites.
            user.addPermissionSatManagementPermission("satellite:*", mutableListOf("viewSatellite"), mutableListOf("*"))

            // User has permission to access all satellites
            with(handleRequest(HttpMethod.Get, "/satellite/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            with(handleRequest(HttpMethod.Get, "/satellite/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }
        }

        @Test
        fun `User can create systems for specific satellites`(): Unit = testApp {
            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite all satellites.
            user.addPermissionSatManagementPermission("satellite:1", mutableListOf("createSystem"), mutableListOf("*"))

            // User can create system on satellite 1
            with(handleRequest(HttpMethod.Post, "/satellite/1/system") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User cannot create system on satellite 2
            with(handleRequest(HttpMethod.Post, "/satellite/2/system") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }
        }

        @Test
        fun `User can view specific systems for specific satellites`(): Unit = testApp {
            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite all satellites.
            user.addPermissionSatManagementPermission(
                "satellite:1",
                mutableListOf("viewSystem"),
                mutableListOf("system:1")
            )

            // User has permission to view system 1 on satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User does not have permission to view system 2 on satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }

            // User does not have permission to view system 1 on satellite 2
            with(handleRequest(HttpMethod.Get, "/satellite/2/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }
        }

        @Test
        fun `Specific child resources permissions work with wildcard`(): Unit = testApp {

            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite all satellites.
            user.addPermissionSatManagementPermission("satellite:1", mutableListOf("viewSystem"), mutableListOf("*"))

            // User has permission to access all system within satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User has permission to access all system within satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User does not have permission to access any system within satellite 2
            with(handleRequest(HttpMethod.Get, "/satellite/2/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.Forbidden
            }
        }

        @Test
        fun `Permissions can be set for multiple actions on multiple resources`(): Unit = testApp {
            val user = creatAuthenticatedUser(roleName = "BasicUser")
            // Add permission to view satellite all satellites.
            user.addPermissionSatManagementPermission(
                "satellite:1",
                mutableListOf("viewSystem", "editSystem"),
                mutableListOf("system:1", "system:2")
            )

            // User has permission to view system 1 on satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User has permission to view system 2 on satellite 1
            with(handleRequest(HttpMethod.Get, "/satellite/1/system/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User has permission to edit system 1 on satellite 1
            with(handleRequest(HttpMethod.Put, "/satellite/1/system/1") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }

            // User has permission to edit system 2 on satellite 1
            with(handleRequest(HttpMethod.Put, "/satellite/1/system/2") {
                addHeader("X-Authz-Token", user.generateAuthZToken())
            }) {
                response.status() `should equal` HttpStatusCode.OK
            }
        }

        private fun creatAuthenticatedUser(
            userId: String = UUID.randomUUID().toString(),
            username: String = UUID.randomUUID().toString(),
            organizationId: String = UUID.randomUUID().toString(),
            roleName: String = "TestRole"
        ): AuthenticatedPrincipalTest {
            return AuthenticatedPrincipalTest(
                userId,
                username,
                organizationId,
                UserRoleTest(roleName, mutableListOf(ServicePermissionTest("satellite-management", mutableListOf())))
            )
        }

        private fun AuthenticatedPrincipalTest.addPermissionSatManagementPermission(
            parentResource: String,
            actions: MutableList<String>,
            resources: MutableList<String>
        ) {
            val servicePermission = role.entitlements.find { it.service == "satellite-management" }!!

            val resourcePermission = servicePermission.resourcePermissions.find { it.parentResource == parentResource }
            if (resourcePermission != null) {
                resourcePermission.permissions.addAll(mutableListOf(ActionTest(actions, resources)))
            } else {
                servicePermission.resourcePermissions.add(
                    ResourcePermissionTest(
                        parentResource,
                        mutableListOf(ActionTest(actions, resources))
                    )
                )
            }
        }

        private fun AuthenticatedPrincipalTest.generateAuthZToken(): String {
            return JsonSettings.minMapper.writeValueAsString(this).customEncodeToBase64String()
        }

        private inner class AuthenticatedPrincipalTest(
            val userId: String,
            val userName: String,
            val organizationId: String,
            val role: UserRoleTest
        ) : Principal

        private inner class UserRoleTest(val name: String, val entitlements: List<ServicePermissionTest>)

        private inner class ServicePermissionTest(
            val service: String,
            val resourcePermissions: MutableList<ResourcePermissionTest>
        )

        private inner class ResourcePermissionTest(val parentResource: String, val permissions: MutableList<ActionTest>)
        private inner class ActionTest(val actions: MutableList<String>, val resources: MutableList<String>)
    }
}
