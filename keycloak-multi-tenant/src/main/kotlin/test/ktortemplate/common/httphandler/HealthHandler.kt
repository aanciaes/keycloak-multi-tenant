package test.ktortemplate.common.httphandler

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.healthHandler() {
    get("/healthz") {
        call.respond(HttpStatusCode.OK)
    }
}
