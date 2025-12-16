package at.yousong.yousong_api.security;

import at.yousong.yousong_api.user.BenutzerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthMeController {

    private final BenutzerRepository repo;

    public AuthMeController(BenutzerRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        // optional: prüfen, ob User noch in DB existiert
        var user = repo.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("authenticated", false));

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", user.getUsername()
        ));
    }
}
