package br.com.jucelio.secureagent.auth;

import br.com.jucelio.secureagent.user.AppUser;
import br.com.jucelio.secureagent.user.AppUserRepository;
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

        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();

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
