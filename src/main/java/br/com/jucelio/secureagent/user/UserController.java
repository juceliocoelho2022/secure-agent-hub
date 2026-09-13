package br.com.jucelio.secureagent.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/me")
    UserProfile me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .sorted()
                .toList();
        return new UserProfile(authentication.getName(), roles);
    }

    record UserProfile(String username, List<String> authorities) {}
}
