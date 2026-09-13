package br.com.jucelio.secureagent.auth;

import br.com.jucelio.secureagent.user.AppUser;
import br.com.jucelio.secureagent.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuthenticationProvider.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DatabaseAuthenticationProvider(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = String.valueOf(authentication.getCredentials());

        log.info("AUTH_DIAG step=provider_enter username={}", username);

        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("AUTH_DIAG step=user_lookup username={} found=false", username);
                    return new BadCredentialsException("Invalid username or password");
                });

        log.info("AUTH_DIAG step=user_lookup username={} found=true enabled={}", username, user.isEnabled());

        if (!user.isEnabled()) {
            log.warn("AUTH_DIAG step=enabled_check username={} result=false", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        log.info("AUTH_DIAG step=password_check username={} matches={}", username, passwordMatches);

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();

        log.info("AUTH_DIAG step=provider_success username={} authorities={}", username, authorities);

        return UsernamePasswordAuthenticationToken.authenticated(
                user.getUsername(),
                null,
                authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
