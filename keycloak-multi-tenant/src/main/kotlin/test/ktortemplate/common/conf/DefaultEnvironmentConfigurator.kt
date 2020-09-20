package test.ktortemplate.common.conf

/* ktlint-disable import-ordering */
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.ktor.application.ApplicationEnvironment
import org.jetbrains.exposed.sql.SchemaUtils
import org.koin.core.module.Module
import org.koin.dsl.module
import test.ktortemplate.authservice.model.KeycloakInterfaceConfigs
import test.ktortemplate.authservice.model.KeycloakSigningKeyResolver
import test.ktortemplate.authservice.service.AuthenticationService
import test.ktortemplate.authservice.service.AuthenticationServiceImpl
import test.ktortemplate.authservice.service.KeycloakEntitlementService
import test.ktortemplate.authservice.service.LocalCacheEntitlementService
import test.ktortemplate.common.conf.database.DatabaseConnection
import test.ktortemplate.usermanagement.model.RoleMappingsTable
import test.ktortemplate.usermanagement.service.ApiKeysService
import test.ktortemplate.usermanagement.service.RoleService
import test.ktortemplate.usermanagement.service.UserService
import javax.sql.DataSource

class DefaultEnvironmentConfigurator(private val environment: ApplicationEnvironment) :
    EnvironmentConfigurator {

    override fun buildEnvironmentConfig(): List<Module> {
        environment.log.info("Init default environment config")

        return listOf(
            initService(),
            initKeycloakInterfaceConfig(),
            initKeycloakSigningKeyResolver(),
            initDbCore()
        )
    }

    private fun initService() = module {
        single<AuthenticationService> { AuthenticationServiceImpl() }
        single { UserService() }
        single { RoleService() }
        single { ApiKeysService() }
        single<KeycloakEntitlementService> { LocalCacheEntitlementService() }
    }

    private fun initKeycloakInterfaceConfig() = module {
        single {
            KeycloakInterfaceConfigs(
                environment.config.property("dev.keycloak.realmFolder").getString(),
                environment.config.propertyOrNull("dev.keycloak.keycloakHost")?.getString()
            )
        }
    }

    private fun initKeycloakSigningKeyResolver() = module {
        single<SigningKeyResolverAdapter> {
            KeycloakSigningKeyResolver(
                environment.config.propertyOrNull("dev.keycloak.keycloakHost")?.getString()
            )
        }
    }

    private fun initDbCore() = module {
        val dataSource: DataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = environment.config.property("dev.datasource.driver").getString()
            jdbcUrl = environment.config.property("dev.datasource.jdbcUrl").getString()
            username = "user"
            password = "test"
            maximumPoolSize = 5
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            leakDetectionThreshold = 10000
            poolName = "ktortemplatepool"
            validate()
        })

        val databaseConnection = DatabaseConnection(dataSource)
        single { databaseConnection }

        bootstrapDatabase(databaseConnection)
    }

    private fun bootstrapDatabase(dbc: DatabaseConnection) {
        dbc.query {
            SchemaUtils.create(RoleMappingsTable)
        }
    }
}
