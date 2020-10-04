package security

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*
import app.User
import java.util.*

private const val validityInMs = 36_000_00 * 10 // 10 hours

@KtorExperimentalAPI
class JwtConfig(private val config: ApplicationConfig) {
    private val secret = config.property("jwt.secret").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val audience = config.property("jwt.audience").getString()
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    /**
     * Produce a token for this combination of User and Account
     */
    fun makeToken(user: User): String = JWT.create()
        .withSubject(user.id.toString())
        .withIssuer(issuer)
        .withAudience(audience)
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    /**
     * Calculate the expiration Date based on current time + the given validity
     */
    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs)

}