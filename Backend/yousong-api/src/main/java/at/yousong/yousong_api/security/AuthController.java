package at.yousong.yousong_api.security;

import at.yousong.yousong_api.user.Benutzer;
import at.yousong.yousong_api.user.BenutzerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {

    private final BenutzerRepository benutzerRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(BenutzerRepository benutzerRepository, BCryptPasswordEncoder encoder, JwtUtil jwtUtil) {
        this.benutzerRepository = benutzerRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        Benutzer user = benutzerRepository.findByUsername(username).orElse(null);
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Bei JWT: Client löscht Token lokal. (Optional: Cookie invalidieren)
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
