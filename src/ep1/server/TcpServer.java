package ep1.server;

import ep1.protocol.Json;
import ep1.protocol.JsonStream;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Cada conexão TCP processa uma requisição e devolve uma resposta JSON. */
public final class TcpServer implements AutoCloseable {
    private final AccountService service;
    private final Consumer<String> log;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "cliente-conectado");
        thread.setDaemon(true);
        return thread;
    });
    private ServerSocket server;
    private Thread acceptThread;

    public TcpServer(AccountService service, Consumer<String> log) {
        this.service = service;
        this.log = log;
    }

    public synchronized int start(int port) throws Exception {
        if (server != null && !server.isClosed()) throw new IllegalStateException("Servidor já iniciado");
        server = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "aceitar-clientes");
        acceptThread.setDaemon(true);
        acceptThread.start();
        log.accept("[SERVIDOR] Iniciado na porta " + server.getLocalPort() + " (todas as interfaces de rede)");
        return server.getLocalPort();
    }

    private void acceptLoop() {
        while (isRunning()) {
            try {
                Socket client = server.accept();
                pool.execute(() -> serve(client));
            } catch (Exception ex) {
                if (isRunning()) log.accept("[ERRO] Falha ao aceitar conexão: " + ex.getMessage());
            }
        }
    }

    private void serve(Socket socket) {
        try (Socket client = socket) {
            client.setSoTimeout(8000);
            String source = client.getRemoteSocketAddress().toString();
            Map<String, Object> answer;
            try {
                String requestText = JsonStream.read(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                log.accept("[RECEBIDO] " + source + " | " + requestText);
                Map<String, Object> request = Json.asObject(Json.parse(requestText));
                answer = service.handle(request);
            } catch (Exception ex) {
                answer = AccountService.response(400, "Requisição JSON inválida", null);
                log.accept("[REQUISIÇÃO INVÁLIDA] " + source + " | " + ex.getMessage());
            }
            String responseText = Json.stringify(answer);
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            output.write(responseText);
            output.newLine();
            output.flush();
            log.accept("[ENVIADO] " + source + " | " + responseText);
        } catch (Exception ex) {
            log.accept("[ERRO] Conexão interrompida: " + ex.getMessage());
        }
    }

    public synchronized boolean isRunning() { return server != null && !server.isClosed(); }
    @Override public synchronized void close() {
        if (server != null) {
            try { server.close(); } catch (Exception ignored) { }
            server = null;
        }
        if (acceptThread != null) acceptThread.interrupt();
        log.accept("[SERVIDOR] Parado");
    }
}
