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
        @NotBlank @Size(min = 4, max = 100)
        public String password;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (benutzerRepository.existsByUsernameIgnoreCase(req.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }
        String hash = encoder.encode(req.password);
        Benutzer saved = benutzerRepository.save(new Benutzer(null, req.username, hash));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "username", saved.getUsername()));
    }
}
