package ep1.client;

import ep1.client.ui.ClientWindow;
import javax.swing.SwingUtilities;

public final class ClientMain {
    private ClientMain() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientWindow().setVisible(true));
    }
}
