import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.identity
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.common.utils.auroraHeaderAuthentication
import test.ktortemplate.satmanagement.httphandler.resourcesRoutes

fun Application.testModule() {

    install(DefaultHeaders)
    install(Compression) {
        gzip {
            priority = 100.0
        }
        identity {
            priority = 10.0
        }
        deflate {
            priority = 1.0
        }
    }

    install(CallLogging)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JsonSettings.mapper))
    }

    install(Authentication) {
        auroraHeaderAuthentication("auroraHeaderAuthentication")
    }

    install(Routing) {
        // Sat Management Mock
        authenticate("auroraHeaderAuthentication") {
            resourcesRoutes()
        }
    }
}

fun <R> testApp(test: TestApplicationEngine.() -> R): R {
    return withTestApplication({
        testModule()
    }, test)
}
