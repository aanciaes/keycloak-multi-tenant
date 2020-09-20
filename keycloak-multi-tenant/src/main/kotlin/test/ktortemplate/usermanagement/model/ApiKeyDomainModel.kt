package test.ktortemplate.usermanagement.model

data class ApiKeyDefinition(
    val id: String,
    val apiKeyName: String,
    val description: String,
    val role: String?
)

data class GenerateApiKeyForm(
    val clientName: String,
    val description: String,
    val role: String,
    val ttl: String
)

data class ApiKeyOutput(
    val clientId: String,
    val clientSecret: String
)
