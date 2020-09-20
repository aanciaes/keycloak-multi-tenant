package test.ktortemplate

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.identity
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import org.koin.ktor.ext.Koin
import test.ktortemplate.authservice.httphandler.authServiceRoutes
import test.ktortemplate.common.conf.DefaultEnvironmentConfigurator
import test.ktortemplate.common.conf.KubernetesEnvironmentConfigurator
import test.ktortemplate.common.httphandler.healthHandler
import test.ktortemplate.common.utils.JsonSettings
import test.ktortemplate.common.utils.auroraHeaderAuthentication
import test.ktortemplate.satmanagement.httphandler.resourcesRoutes
import test.ktortemplate.usermanagement.httphandler.apiKeysHandler
import test.ktortemplate.usermanagement.httphandler.rolesHandler
import test.ktortemplate.usermanagement.httphandler.usersHandler

@KtorExperimentalAPI
val Application.env
    get() = environment.config.property("ktor.environment").getString()

@KtorExperimentalAPI
fun Application.module() {

    val modules = when (envKind) {
        "kube" -> KubernetesEnvironmentConfigurator(environment).buildEnvironmentConfig()
        else -> DefaultEnvironmentConfigurator(environment).buildEnvironmentConfig()
    }

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

    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JsonSettings.mapper))
    }
    install(CORS) {
        anyHost()

        method(HttpMethod.Options)
        method(HttpMethod.Patch)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)

        header(HttpHeaders.Authorization)

        allowCredentials = true
    }

    install(Koin) {
        modules(modules)
    }

    install(Authentication) {
        auroraHeaderAuthentication("auroraHeaderAuthentication")
    }

    install(Routing) {
        // Ambassador validation routes
        authServiceRoutes()
        healthHandler()

        // User Service
        authenticate("auroraHeaderAuthentication") {
            usersHandler()
            rolesHandler()
            resourcesRoutes()
            apiKeysHandler()
        }

        // Sat Management Mock
        authenticate("auroraHeaderAuthentication") {
            resourcesRoutes()
        }
    }

    log.info("Ktor server started...")
}

val Application.envKind get() = environment.config.property("ktor.environment").getString()
