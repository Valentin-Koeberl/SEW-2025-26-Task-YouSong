package at.yousong.yousong_api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final Algorithm algorithm;
    private final long expirationMs;

    public JwtUtil(
            @Value("${yousong.jwt.secret}") String secret,
            @Value("${yousong.jwt.expiration}") long expirationMs
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .withIssuer("YouSong")
                .sign(algorithm);
    }

    public String validateAndExtractUsername(String token) {
        DecodedJWT jwt = JWT.require(algorithm).withIssuer("YouSong").build().verify(token);
        return jwt.getSubject();
    }
}
