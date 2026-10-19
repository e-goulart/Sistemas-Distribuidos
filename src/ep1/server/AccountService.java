package ep1.server;

import ep1.protocol.Json;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Regras de negócio do contrato público definido na planilha e no PDF. */
public final class AccountService {
    private static final Pattern NAME = Pattern.compile("[A-Za-z ]{1,60}");
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._]{3,20}");
    private static final Pattern PASSWORD = Pattern.compile("[A-Za-z0-9#.*&%$@!()_+=-]{8,20}");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[#.*&%$@!()_+=-]");
    private final UserRepository users;
    private final Map<String, String> sessions = new HashMap<>();

    public AccountService(Path file) throws IOException { users = new UserRepository(file); }

    public synchronized Map<String, Object> handle(Map<String, Object> request) {
        try {
            String method = Json.field(request, "method");
            Object payload = request.get("data");
            if (method == null || !method.equals(method.toLowerCase(java.util.Locale.ROOT))) {
                return response(400, "O método deve ser informado em letras minúsculas", null);
            }
            if (!(payload instanceof Map<?, ?>)) return response(400, "O campo data deve ser um objeto JSON", null);
            Map<String, Object> data = Json.asObject(payload);
            switch (method) {
                case "register": return register(data);
                case "login": return login(data);
                case "logout": return logout(data);
                case "getuser": return getUser(data);
                case "updateusername": return updateName(data);
                case "updateuserpassword": return updatePassword(data);
                case "deleteuser": return deleteUser(data);
                default: return response(400, "Método não reconhecido: " + method, null);
            }
        } catch (IllegalArgumentException ex) {
            return response(400, "Requisição inválida: " + ex.getMessage(), null);
        } catch (IOException ex) {
            return response(500, "Falha ao salvar os dados do usuário", null);
        } catch (Exception ex) {
            return response(500, "Erro interno do servidor", null);
        }
    }

    private Map<String, Object> register(Map<String, Object> data) throws IOException {
        String name = Json.field(data, "name");
        String username = Json.field(data, "username");
        String password = Json.field(data, "password");
        if (username != null && users.exists(username)) return response(409, "O username já está em uso.", null);
        if (!validName(name)) return invalid("name");
        if (username == null || !USERNAME.matcher(username).matches()) return invalid("username");
        if (!validPassword(password)) return invalid("password");
        users.put(new UserRepository.User(name, username, Passwords.encode(password)));
        return response(201, "Usuário criado com sucesso", null);
    }

    private Map<String, Object> login(Map<String, Object> data) {
        String username = Json.field(data, "username");
        String password = Json.field(data, "password");
        if (username == null || password == null) return response(400, "Informe username e password", null);
        UserRepository.User user = users.get(username);
        if (user == null || !Passwords.matches(password, user.secret)) {
            return response(401, "Usuario ou senhas inválidos", null);
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return response(200, "Sucesso no Login", Json.object("token", token));
    }

    private Map<String, Object> logout(Map<String, Object> data) {
        String token = Json.field(data, "token");
        Map<String, Object> problem = tokenProblem(token);
        if (problem != null) return problem;
        sessions.remove(token);
        return response(200, "Usuário deslogado com sucesso", null);
    }

    private Map<String, Object> getUser(Map<String, Object> data) {
        Map<String, Object> problem = authorizationProblem(data);
        if (problem != null) return problem;
        UserRepository.User user = users.get(Json.field(data, "username"));
        if (user == null) return response(404, "Usuário não encontrado", null);
        return response(200, "Usuário encontrado com sucesso", Json.object("name", user.name, "username", user.username));
    }

    private Map<String, Object> updateName(Map<String, Object> data) throws IOException {
        Map<String, Object> problem = authorizationProblem(data);
        if (problem != null) return problem;
        String name = Json.field(data, "name");
        if (!validName(name)) return response(400, "Nome do usuário fora do padrão", null);
        UserRepository.User user = users.get(Json.field(data, "username"));
        if (user == null) return response(404, "Usuário não encontrado", null);
        users.updateName(user, name);
        return response(200, "Nome do usuário atualizado com sucesso", null);
    }

    private Map<String, Object> updatePassword(Map<String, Object> data) throws IOException {
        Map<String, Object> problem = authorizationProblem(data);
        if (problem != null) return problem;
        UserRepository.User user = users.get(Json.field(data, "username"));
        if (user == null) return response(404, "Usuário não encontrado", null);
        String oldPassword = Json.field(data, "oldPassword");
        String newPassword = Json.field(data, "newPassword");
        if (oldPassword == null || !Passwords.matches(oldPassword, user.secret)) {
            return response(401, "Senha atual incorreta", null);
        }
        if (!validPassword(newPassword)) return response(400, "Senha fora do padrão", null);
        if (Passwords.matches(newPassword, user.secret)) return response(400, "A senha nova é igual à senha antiga", null);
        users.updateSecret(user, Passwords.encode(newPassword));
        return response(200, "Senha atualizada com sucesso", null);
    }

    private Map<String, Object> deleteUser(Map<String, Object> data) throws IOException {
        Map<String, Object> problem = authorizationProblem(data);
        if (problem != null) return problem;
        String username = Json.field(data, "username");
        if (!users.exists(username)) return response(404, "Usuário não encontrado", null);
        users.remove(username);
        for (Iterator<Map.Entry<String, String>> it = sessions.entrySet().iterator(); it.hasNext();) {
            if (it.next().getValue().equals(username)) it.remove();
        }
        return response(200, "Usuário deletado com sucesso", null);
    }

    private Map<String, Object> authorizationProblem(Map<String, Object> data) {
        String token = Json.field(data, "token");
        Map<String, Object> tokenError = tokenProblem(token);
        if (tokenError != null) return tokenError;
        String username = Json.field(data, "username");
        if (username == null || !username.equals(sessions.get(token))) {
            return response(401, "Operação não autorizada", null);
        }
        return null;
    }

    private Map<String, Object> tokenProblem(String token) {
        if (token == null || token.isBlank()) return response(400, "Token de autenticação não fornecido.", null);
        if (!sessions.containsKey(token)) return response(401, "Sessão expirada ou encerrada.", null);
        return null;
    }

    private static boolean validName(String text) {
        return text != null && NAME.matcher(text).matches() && !text.isBlank();
    }
    private static boolean validPassword(String text) {
        return text != null && PASSWORD.matcher(text).matches()
                && UPPER.matcher(text).find() && LOWER.matcher(text).find()
                && DIGIT.matcher(text).find() && SYMBOL.matcher(text).find();
    }
    private static Map<String, Object> invalid(String field) {
        return response(400, "O campo " + field + " não está no padrão esperado.", null);
    }
    public static Map<String, Object> response(int status, String message, Object data) {
        return Json.object("statusCode", status, "message", message, "data", data);
    }
}
