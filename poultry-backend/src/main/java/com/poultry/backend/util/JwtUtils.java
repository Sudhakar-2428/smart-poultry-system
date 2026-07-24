package com.poultry.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import com.poultry.backend.entity.User;
import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.MembershipStatus;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.repository.FarmMemberRepository;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtils {

    @Autowired
    @Lazy
    private FarmMemberRepository farmMemberRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;


    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Get the configured token expiration time in milliseconds.
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Retrieve username from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Retrieve expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a single claim from token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if a token is expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate token for a specific UserDetails.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add roles/authorities to claims
        claims.put("roles", userDetails.getAuthorities());

        if (userDetails instanceof CustomUserDetails) {
            User user = ((CustomUserDetails) userDetails).getUser();
            claims.put("userId", user.getId());

            Long farmId = null;
            String farmRole = null;
            String membershipStatus = null;

            if (farmMemberRepository != null) {
                List<FarmMember> memberships = farmMemberRepository.findByUserId(user.getId());
                Optional<FarmMember> activeMembership = memberships.stream()
                        .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                        .findFirst();

                if (!activeMembership.isPresent()) {
                    activeMembership = memberships.stream()
                            .filter(m -> m.getStatus() == MembershipStatus.PENDING)
                            .findFirst();
                }

                if (activeMembership.isPresent()) {
                    FarmMember m = activeMembership.get();
                    farmId = m.getFarm().getId();
                    farmRole = m.getRole().name();
                    membershipStatus = m.getStatus().name();
                }
            }

            claims.put("farmId", farmId);
            claims.put("farmRole", farmRole);
            claims.put("membershipStatus", membershipStatus);
        }

        return createToken(claims, userDetails.getUsername());
    }


    /**
     * Create tokens using claims and subject.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Validate token against UserDetails.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token trace: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get security Key object from base64 secret config.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException ignored) {
                // Keep keyBytes as is
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
