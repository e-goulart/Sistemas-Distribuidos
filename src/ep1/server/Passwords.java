package ep1.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Senhas armazenadas como hash com salt, nunca no arquivo em texto puro. */
final class Passwords {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Passwords() { }
    static String encode(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash(password, salt));
    }
    static boolean matches(String password, String encoded) {
        try {
            String[] parts = encoded.split(":", -1);
            if (parts.length != 2) return false;
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            return MessageDigest.isEqual(expected, hash(password, salt));
        } catch (RuntimeException ex) { return false; }
    }
    private static byte[] hash(String password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, 80_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded(); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível verificar a senha", ex); }
        finally { specification.clearPassword(); }
    }
}
