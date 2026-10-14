package ep1.server;

import ep1.protocol.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistência em arquivo local independente das rotinas de TCP. */
final class UserRepository {
    static final class User {
        String name;
        final String username;
        String secret;
        User(String name, String username, String secret) {
            this.name = name;
            this.username = username;
            this.secret = secret;
        }
    }
    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();

    UserRepository(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) load();
    }
    User get(String username) { return users.get(username); }
    boolean exists(String username) { return users.containsKey(username); }
    void put(User user) throws IOException {
        users.put(user.username, user);
        try { save(); }
        catch (IOException ex) { users.remove(user.username); throw ex; }
    }
    void updateName(User user, String name) throws IOException {
        String old = user.name;
        user.name = name;
        try { save(); }
        catch (IOException ex) { user.name = old; throw ex; }
    }
    void updateSecret(User user, String newSecret) throws IOException {
        String old = user.secret;
        user.secret = newSecret;
        try { save(); }
        catch (IOException ex) { user.secret = old; throw ex; }
    }
    void remove(String username) throws IOException {
        User removed = users.remove(username);
        try { save(); }
        catch (IOException ex) { if (removed != null) users.put(username, removed); throw ex; }
    }
    private void load() throws IOException {
        try {
            Object parsed = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
            if (!(parsed instanceof List<?>)) throw new IllegalArgumentException("Esperada lista de usuários");
            for (Object element : (List<?>) parsed) {
                Map<String, Object> data = Json.asObject(element);
                String name = Json.field(data, "name");
                String username = Json.field(data, "username");
                String hash = Json.field(data, "passwordHash");
                if (name == null || username == null || hash == null) throw new IllegalArgumentException("Arquivo de usuário inválido");
                users.put(username, new User(name, username, hash));
            }
        } catch (RuntimeException ex) { throw new IOException("Arquivo de usuários inválido: " + file, ex); }
    }
    private void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        List<Object> records = new java.util.ArrayList<>();
        for (User user : users.values()) {
            records.add(Json.object("name", user.name, "username", user.username, "passwordHash", user.secret));
        }
        Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "users-", ".tmp");
        try {
            Files.writeString(temp, Json.stringify(records), StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
