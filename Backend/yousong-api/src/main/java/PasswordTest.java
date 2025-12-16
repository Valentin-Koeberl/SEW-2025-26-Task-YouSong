import org.testng.annotations.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.testng.AssertJUnit.assertTrue;

public class PasswordTest {

    @Test
    void checkPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$ezgppxa8lzyTAX2.GJxcoudLSWdUmEZ.XcsxrCxAiFVZnsB3fF3t.";
        boolean matches = encoder.matches("password", hash);

        System.out.println("Passt? " + matches);
        assertTrue(matches);
    }
}