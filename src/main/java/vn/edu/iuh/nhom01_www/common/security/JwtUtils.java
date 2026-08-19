package vn.edu.iuh.nhom01_www.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    public String generateToken(UserDetails userDetails) {
        java.util.List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(java.util.stream.Collectors.toList());

        Long version = 0L;
        if (userDetails instanceof CustomUserDetails) {
            Long v = ((CustomUserDetails) userDetails).getVersion();
            if (v != null) version = v;
        }

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim("version", version)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) key()).build()
               .parseSignedClaims(token).getPayload().getSubject();
    }

    public Long getVersionFromToken(String token) {
        Object version = Jwts.parser().verifyWith((javax.crypto.SecretKey) key()).build()
                .parseSignedClaims(token).getPayload().get("version");
        if (version instanceof Number) {
            return ((Number) version).longValue();
        }
        return 0L;
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith((javax.crypto.SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log error
        }
        return false;
    }
}
