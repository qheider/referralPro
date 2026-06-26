import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String plainPassword = "password123";
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye8JonxY8J2m8iQV9ZE.x4MR3fczYV8fO";
        System.out.println("Matches: " + encoder.matches(plainPassword, storedHash));
        System.out.println("New hash: " + encoder.encode(plainPassword));
    }
}
