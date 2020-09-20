package test.ktortemplate.common.model

data class ResponseEntity(val content: String)

class ErrorResponseEntity(val error: String?)
class AuthenticationFailed(message: String) : Exception(message)
class AuthorizationFailed(message: String) : Exception(message)

class UserNotFound(message: String) : Exception(message)
class RoleNotFound(message: String) : Exception(message)
class UsernameAlreadyUsed(message: String) : Exception(message)
class SessionNotFound(message: String) : Exception(message)
class EmailNotSentError(message: String) : Exception(message)
class ApiKeyNotFound(message: String) : Exception(message)
