package ep1.client;

import ep1.protocol.Json;
import ep1.protocol.JsonStream;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/** Cliente de rede independente da interface. Uma nova conexão por chamada. */
public final class RemoteApi {
    private final Consumer<String> logger;
    public RemoteApi(Consumer<String> logger) { this.logger = logger; }

    public Map<String, Object> invoke(String host, int port, Map<String, Object> request) throws Exception {
        String outgoing = Json.stringify(request);
        return invokeJson(host, port, outgoing);
    }

    public Map<String, Object> invokeJson(String host, int port, String outgoing) throws Exception {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("IP ou hostname não informado");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Porta fora do intervalo 1-65535");
        logger.accept("[CLIENTE → SERVIDOR] " + host + ":" + port + " | " + outgoing);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.trim(), port), 5000);
            socket.setSoTimeout(8000);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(outgoing);
            writer.newLine();
            writer.flush();
            String incoming = JsonStream.read(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            logger.accept("[SERVIDOR → CLIENTE] " + incoming);
            return Json.asObject(Json.parse(incoming));
        }
    }
}
