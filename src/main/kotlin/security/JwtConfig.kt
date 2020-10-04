package security

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*
import service.User
import java.util.*

@KtorExperimentalAPI
object JwtConfig {
    private val secret = HoconApplicationConfig(ConfigFactory.load("application.local.conf"))
        .property("security.jwt.secret")
        .getString()
    private const val issuer = "postithere.com"
    private const val validityInMs = 36_000_00 * 10 // 10 hours
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    /**
     * Produce a token for this combination of User and Account
     */
    fun makeToken(user: User): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", user.id.toString())
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    /**
     * Calculate the expiration Date based on current time + the given validity
     */
    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs)

}