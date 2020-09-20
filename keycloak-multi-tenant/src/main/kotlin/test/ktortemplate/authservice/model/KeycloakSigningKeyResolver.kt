package test.ktortemplate.authservice.model

/* ktlint-disable import-ordering */
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.SigningKeyResolverAdapter
import khttp.get
import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

class KeycloakSigningKeyResolver(private val keycloakHostOverwrite: String?) : SigningKeyResolverAdapter() {

    private val signingKeys = mutableMapOf<String, Key>()

    override fun resolveSigningKey(header: JwsHeader<out JwsHeader<*>>?, claims: Claims?): Key {

        val keyId = header?.getKeyId()
        return if (signingKeys.containsKey(keyId)) {
            signingKeys[keyId]!!
        } else {
            println("Key not present in cache, retrieving jwk from auth server")
            val issuer = claims?.issuer ?: throw Exception("No issuer present in token")
            val newEntries = retrieveJWKForRealm(issuer)

            if (newEntries.isEmpty() || !newEntries.containsKey(keyId)) {
                throw Exception("No new entries match kid requested")
            } else {
                signingKeys.putAll(newEntries)
                signingKeys[keyId]!!
            }
        }
    }

    private fun retrieveJWKForRealm(issuer: String): Map<String, Key> {
        try {
            // WARNING: This should be a request to the issuer. Overwriting keycloak host because running inside cluster without global dns name
            val res = if (keycloakHostOverwrite != null) {
                val buildHost = issuer.replace(Regex("http://(.*):8080"), keycloakHostOverwrite)
                get("$buildHost/protocol/openid-connect/certs").jsonObject
            } else {
                get("$issuer/protocol/openid-connect/certs").jsonObject
            }
            val output = mutableMapOf<String, Key>()
            val keyList = res.getJSONArray("keys")

            for (i in 0 until keyList.length()) {
                val key = keyList.getJSONObject(i)

                val kId = key.getString("kid")
                val modulusStr = key.getString("n")
                val exponentStr = key.getString("e")

                val modulus = BigInteger(1, Base64.getUrlDecoder().decode(modulusStr))
                val publicExponent = BigInteger(1, Base64.getUrlDecoder().decode(exponentStr))

                val keyFactory = KeyFactory.getInstance("RSA")
                val publicKey = keyFactory.generatePublic(RSAPublicKeySpec(modulus, publicExponent))

                output[kId] = publicKey
            }

            println("keys retrieved")
            return output
        } catch (e: Exception) {
            e.printStackTrace()
            return mapOf()
        }
    }
}
