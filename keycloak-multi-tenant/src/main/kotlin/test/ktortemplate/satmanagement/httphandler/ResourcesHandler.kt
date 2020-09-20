package test.ktortemplate.satmanagement.httphandler

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import test.ktortemplate.common.utils.AuthHandlerUtils.fineGrainedAuthorization

fun Route.resourcesRoutes() {

    get("/satellite") {
        fineGrainedAuthorization(
            "satellite-management",
            "satellite",
            "listSatellite",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    post("/satellite") {
        fineGrainedAuthorization(
            "satellite-management",
            "satellite",
            "createSatellite",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    get("/satellite/{satellite-id}") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "viewSatellite",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    put("/satellite/{satellite-id}") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "editSatellite",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    delete("/satellite") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite",
            "deleteSatellite",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    get("/satellite/{satellite-id}/system") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "listSystem",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    post("/satellite/{satellite-id}/system") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "createSystem",
            ""
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    get("/satellite/{satellite-id}/system/{system-id}") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        val systemId = call.parameters["system-id"]
        requireNotNull(systemId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "viewSystem",
            "system:$systemId"
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }

    put("/satellite/{satellite-id}/system/{system-id}") {
        val satelliteId = call.parameters["satellite-id"]
        requireNotNull(satelliteId)

        val systemId = call.parameters["system-id"]
        requireNotNull(systemId)

        fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "editSystem",
            "system:$systemId"
        ) { authorizedUser ->
            call.respond(HttpStatusCode.OK, authorizedUser)
        }
    }
}
