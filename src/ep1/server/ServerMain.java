package ep1.server;

import ep1.server.ui.ServerWindow;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.SwingUtilities;

public final class ServerMain {
    private ServerMain() { }
    public static void main(String[] args) throws Exception {
        //Path storage = Path.of("dados", "usuarios.json");
        Path storage = Path.of("SistemaDistribuido_EP1_", "dados", "usuarios.json");
        if (Arrays.asList(args).contains("--console") || GraphicsEnvironment.isHeadless()) {
            int port = args.length > 1 && "--console".equals(args[0]) ? Integer.parseInt(args[1]) : 22072;
            TcpServer listener = new TcpServer(new AccountService(storage), System.out::println);
            listener.start(port);
            Runtime.getRuntime().addShutdownHook(new Thread(listener::close));
            new java.util.concurrent.CountDownLatch(1).await();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { new ServerWindow(storage).setVisible(true); }
            catch (Exception ex) { javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage(), "Erro", javax.swing.JOptionPane.ERROR_MESSAGE); }
        });
    }
}
