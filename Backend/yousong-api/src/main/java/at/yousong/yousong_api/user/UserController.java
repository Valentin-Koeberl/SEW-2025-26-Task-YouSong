package at.yousong.yousong_api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final BenutzerRepository benutzerRepository;
    private final BCryptPasswordEncoder encoder;

    public UserController(BenutzerRepository benutzerRepository, BCryptPasswordEncoder encoder) {
        this.benutzerRepository = benutzerRepository;
        this.encoder = encoder;
    }

    public static class CreateUserRequest {
        @NotBlank @Size(min = 3, max = 100)
        public String username;
        // Accept plain password, but also allow legacy "passwordHash" field as alias from older clients
        @NotBlank @Size(min = 4, max = 100)
        public String password;
        public String passwordHash; // optional alias; if present we use it as plain text input
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (benutzerRepository.existsByUsernameIgnoreCase(req.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }
        String raw = req.password != null ? req.password : req.passwordHash;
        String hash = encoder.encode(raw);
        Benutzer saved = benutzerRepository.save(new Benutzer(null, req.username, hash));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "username", saved.getUsername()));
    }
}
