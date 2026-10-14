package ep1.tests;

import ep1.client.RemoteApi;
import ep1.protocol.Json;
import ep1.server.AccountService;
import ep1.server.TcpServer;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Testes de integração TCP reais, sem framework, no próprio computador. */
public final class ProtocolTests {
    private static int passed;
    private static RemoteApi api;
    private static int port;
    private static final String PASSWORD = "Senha123!";
    private static final String NEW_PASSWORD = "Nova123!";

    private ProtocolTests() { }
    public static void main(String[] args) throws Exception {
        Path testDir = Files.createTempDirectory("ep1-teste-");
        Path storage = testDir.resolve("usuarios.json");
        api = new RemoteApi(message -> { });
        TcpServer listener = new TcpServer(new AccountService(storage), message -> { });
        try {
            port = listener.start(0);
            check("metodo inválido", call("desconhecido", Json.object()), 400);
            check("registro sem nome", call("register", Json.object("name", "", "username", "tester", "password", PASSWORD)), 400);
            check("registro nome numerico", call("register", Json.object("name", "Nome1", "username", "tester", "password", PASSWORD)), 400);
            check("registro username fora padrao", call("register", Json.object("name", "Nome Teste", "username", "AAa", "password", PASSWORD)), 400);
            check("registro senha sem especial", call("register", Json.object("name", "Nome Teste", "username", "tester", "password", "Senha123")), 400);
            check("registro inicial", call("register", Json.object("name", "Nome Teste", "username", "tester", "password", PASSWORD)), 201);
            check("registro duplicado", call("register", Json.object("name", "Outro Nome", "username", "tester", "password", PASSWORD)), 409);
            check("login com senha errada", call("login", Json.object("username", "tester", "password", "ERRADO")), 401);
            check("login sem usuário", call("login", Json.object("username", "inexistente", "password", PASSWORD)), 401);
            Map<String, Object> login = call("login", Json.object("username", "tester", "password", PASSWORD));
            check("login correto", login, 200);
            String token = Json.field(Json.asObject(login.get("data")), "token");
            require("token gerado como UUID", token != null && UUID.fromString(token).toString().equals(token));
            check("logout sem token", call("logout", Json.object()), 400);
            check("consulta token inválido", call("getuser", Json.object("token", UUID.randomUUID().toString(), "username", "tester")), 401);
            check("consulta outro cadastro bloqueada", call("getuser", Json.object("token", token, "username", "outro")), 401);
            Map<String, Object> consultation = call("getuser", Json.object("token", token, "username", "tester"));
            check("consulta própria", consultation, 200);
            require("consulta só apresenta nome e username", consultation.get("data") instanceof Map && Json.asObject(consultation.get("data")).size() == 2 && "Nome Teste".equals(Json.field(Json.asObject(consultation.get("data")), "name")));
            check("atualizar nome inválido", call("updateusername", Json.object("token", token, "username", "tester", "name", "Teste12")), 400);
            check("atualizar nome correto", call("updateusername", Json.object("token", token, "username", "tester", "name", "Nome Corrigido")), 200);
            check("alterar senha errada", call("updateuserpassword", Json.object("token", token, "username", "tester", "oldPassword", "errada", "newPassword", NEW_PASSWORD)), 401);
            check("alterar senha fraca", call("updateuserpassword", Json.object("token", token, "username", "tester", "oldPassword", PASSWORD, "newPassword", "12345678")), 400);
            check("alterar senha correto", call("updateuserpassword", Json.object("token", token, "username", "tester", "oldPassword", PASSWORD, "newPassword", NEW_PASSWORD)), 200);
            check("senha antiga recusada", call("login", Json.object("username", "tester", "password", PASSWORD)), 401);
            check("senha nova aceita", call("login", Json.object("username", "tester", "password", NEW_PASSWORD)), 200);
            check("exclusão outro username bloqueada", call("deleteuser", Json.object("token", token, "username", "notme")), 401);
            check("logout", call("logout", Json.object("token", token)), 200);
            check("token revogado", call("getuser", Json.object("token", token, "username", "tester")), 401);
            Map<String, Object> again = call("login", Json.object("username", "tester", "password", NEW_PASSWORD));
            check("login novamente", again, 200);
            String token2 = Json.field(Json.asObject(again.get("data")), "token");
            require("token novo diferente", !token.equals(token2));
            check("remover cadastro", call("deleteuser", Json.object("token", token2, "username", "tester")), 200);
            check("não loga depois exclusão", call("login", Json.object("username", "tester", "password", NEW_PASSWORD)), 401);
            check("token revogado depois exclusão", call("getuser", Json.object("token", token2, "username", "tester")), 401);
            check("malformed json", api.invokeJson("127.0.0.1", port, "{errado}"), 400);
            check("registro de usuário para persistência", call("register", Json.object("name", "Persistido", "username", "persistido", "password", PASSWORD)), 201);
            check("sem newline também funciona", sendWithoutNewline("{\"method\":\"login\",\"data\":{\"username\":\"persistido\",\"password\":\"Senha123!\"}}"), 200);
        } finally { listener.close(); }
        TcpServer restarted = new TcpServer(new AccountService(storage), message -> { });
        try {
            port = restarted.start(0);
            check("persiste após reiniciar", call("login", Json.object("username", "persistido", "password", PASSWORD)), 200);
        } finally { restarted.close(); }
        require("arquivo salvo em disco", Files.exists(storage));
        String saved = Files.readString(storage);
        require("senha não aparece em texto puro no disco", !saved.contains(PASSWORD));
        System.out.println("\nOK — " + passed + " verificações concluídas. Testes reais por TCP.");
    }

    private static Map<String, Object> sendWithoutNewline(String json) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(5000);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(json);
            writer.flush();
            String answer = ep1.protocol.JsonStream.read(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            return Json.asObject(Json.parse(answer));
        }
    }
    private static Map<String, Object> call(String method, Map<String, Object> data) throws Exception {
        return api.invoke("127.0.0.1", port, Json.object("method", method, "data", data));
    }
    private static void check(String label, Map<String, Object> answer, int code) {
        Object status = answer.get("statusCode");
        require(label + " (esperado " + code + ", obtido " + status + ")", status instanceof Number && ((Number) status).intValue() == code);
        require("estrutura completa de resposta: " + label, answer.containsKey("message") && answer.containsKey("data"));
    }
    private static void require(String label, boolean result) {
        if (!result) throw new AssertionError("FALHOU: " + label);
        passed++;
        System.out.println("[OK] " + label);
    }
}
