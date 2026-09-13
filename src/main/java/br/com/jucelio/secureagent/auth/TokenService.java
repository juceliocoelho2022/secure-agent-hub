package br.com.jucelio.secureagent.auth;

import br.com.jucelio.secureagent.user.AppUser;
import br.com.jucelio.secureagent.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public TokenService(JwtEncoder jwtEncoder,
                        AppUserRepository users,
                        RefreshTokenRepository refreshTokens,
                        @Value("${app.security.jwt.access-ttl:15m}") Duration accessTtl,
                        @Value("${app.security.jwt.refresh-ttl:7d}") Duration refreshTtl) {
        this.jwtEncoder = jwtEncoder;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public TokenResponse issue(String username) {
        AppUser user = users.findByUsername(username).orElseThrow();
        String accessToken = createAccessToken(user);
        String opaqueRefresh = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokens.save(new RefreshToken(user, sha256(opaqueRefresh), Instant.now().plus(refreshTtl)));
        return new TokenResponse("Bearer", accessToken, accessTtl.toSeconds(), opaqueRefresh);
    }

    @Transactional
    public TokenResponse rotate(String opaqueRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(opaqueRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!stored.isValid(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }
        stored.revoke();
        return issue(stored.getUser().getUsername());
    }

    @Transactional
    public void revoke(String opaqueRefreshToken) {
        refreshTokens.findByTokenHash(sha256(opaqueRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    private String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        var authorities = user.getRoles().stream().map(r -> "ROLE_" + r.getName()).sorted().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("secure-agent-hub")
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .subject(user.getUsername())
                .claim("authorities", authorities)
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256)
                .keyId("secure-agent-hub-hs256")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
