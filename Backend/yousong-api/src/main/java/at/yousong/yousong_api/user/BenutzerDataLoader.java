package at.yousong.yousong_api.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"default"})
public class BenutzerDataLoader implements CommandLineRunner {

    private final BenutzerRepository benutzerRepository;
    private final BCryptPasswordEncoder encoder;

    public BenutzerDataLoader(BenutzerRepository benutzerRepository, BCryptPasswordEncoder encoder) {
        this.benutzerRepository = benutzerRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String username = "hugo";
        String hash = encoder.encode("password");
        benutzerRepository.findByUsername(username)
                .ifPresentOrElse(user -> {
                    user.setPasswordHash(hash); // reset to known demo password
                    benutzerRepository.save(user);
                }, () -> benutzerRepository.save(new Benutzer(null, username, hash)));
    }
}
