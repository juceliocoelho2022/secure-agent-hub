package br.com.jucelio.secureagent.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.bootstrap.dev-users", havingValue = "true", matchIfMissing = true)
public class DevUserBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public DevUserBootstrap(AppUserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDevUser("analyst", "analyst123", "ANALYST");
        ensureDevUser("operator", "operator123", "OPERATOR");
        ensureDevUser("auditor", "auditor123", "AUDITOR");
        ensureDevUser("admin", "admin123", "ADMIN");
    }

    private void ensureDevUser(String username, String password, String roleName) {
        var existing = users.findByUsername(username);
        if (existing.isPresent()) {
            AppUser user = existing.get();
            if (!encoder.matches(password, user.getPasswordHash())) {
                user.resetPasswordHash(encoder.encode(password));
                users.save(user);
            }
            return;
        }

        Role role = roles.findByName(roleName).orElseThrow();
        users.save(new AppUser(username, encoder.encode(password), Set.of(role)));
    }
}
