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
        createIfMissing("analyst", "analyst123", "ANALYST");
        createIfMissing("operator", "operator123", "OPERATOR");
        createIfMissing("auditor", "auditor123", "AUDITOR");
        createIfMissing("admin", "admin123", "ADMIN");
    }

    private void createIfMissing(String username, String password, String roleName) {
        if (users.existsByUsername(username)) return;
        Role role = roles.findByName(roleName).orElseThrow();
        users.save(new AppUser(username, encoder.encode(password), Set.of(role)));
    }
}
