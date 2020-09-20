package test.ktortemplate.usermanagement.model

import org.jetbrains.exposed.sql.Table

internal object RoleMappingsTable : Table("role") {
    val id = long("id").primaryKey().autoIncrement()
    val name = text("name")
    val description = text("description")
    val organizationId = text("organizationId")
    val entitlements = text("entitlements")
}
