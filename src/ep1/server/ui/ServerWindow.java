package ep1.server.ui;

import ep1.server.AccountService;
import ep1.server.TcpServer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class ServerWindow extends JFrame {
    private final Color blue = new Color(32, 86, 170);
    private final JTextField port = new JTextField("34345", 9);
    private final JLabel state = new JLabel("● Desligado");
    private final JButton toggle = new JButton("Iniciar servidor");
    private final JTextArea logs = new JTextArea();
    private final AccountService accounts;
    private TcpServer listener;

    public ServerWindow(Path storage) throws Exception {
        super("EP1 • Central do Servidor");
        accounts = new AccountService(storage);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 650);
        setMinimumSize(new Dimension(680, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(246, 248, 253));
        setLayout(new BorderLayout(0, 12));

        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setBackground(new Color(24, 44, 77));
        heading.setBorder(BorderFactory.createEmptyBorder(23, 28, 21, 28));
        JLabel title = new JLabel("SERVIDOR  /  GESTÃO DE USUÁRIOS");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Comunicação TCP · protocolo JSON · operações de autenticação e cadastro");
        subtitle.setForeground(new Color(194, 215, 242));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 22, 22, 22));
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 7, 4, 7);
        c.gridy = 0; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
        controls.add(new JLabel("Porta TCP:"), c);
        c.gridx = 1;
        port.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        controls.add(port, c);
        c.gridx = 2;
        toggle.setBackground(blue);
        toggle.setForeground(Color.WHITE);
        toggle.setOpaque(true);
        toggle.setFocusPainted(false);
        toggle.addActionListener(e -> switchState());
        controls.add(toggle, c);
        c.gridx = 3;
        state.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        state.setForeground(new Color(160, 60, 60));
        controls.add(state, c);
        c.gridy = 1; c.gridx = 0; c.gridwidth = 4;
        JLabel helper = new JLabel("Servidor disponível em 0.0.0.0:PORTA (clientes usam o IP real desta máquina).");
        helper.setForeground(new Color(83, 100, 125));
        controls.add(helper, c);
        content.add(controls, BorderLayout.NORTH);

        JPanel logBox = new JPanel(new BorderLayout(0, 9));
        logBox.setOpaque(false);
        JLabel logTitle = new JLabel("Monitor de requisições e respostas");
        logTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        logBox.add(logTitle, BorderLayout.NORTH);
        logs.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logs.setBackground(new Color(15, 26, 45));
        logs.setForeground(new Color(221, 235, 252));
        logs.setCaretColor(Color.WHITE);
        logs.setEditable(false);
        logs.setLineWrap(true);
        logs.setWrapStyleWord(true);
        logs.setMargin(new Insets(13, 13, 13, 13));
        logBox.add(new JScrollPane(logs), BorderLayout.CENTER);
        content.add(logBox, BorderLayout.CENTER);
        JButton clear = new JButton("Limpar histórico da tela");
        clear.addActionListener(e -> logs.setText(""));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(new JLabel("Cada requisição abre uma conexão TCP independente. A resposta encerra a conexão."), BorderLayout.CENTER);
        footer.add(clear, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        note("Interface iniciada. Escolha a porta e clique em Iniciar servidor.");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { if (listener != null) listener.close(); }
        });
    }

    private void switchState() {
        if (listener != null && listener.isRunning()) {
            listener.close();
            listener = null;
            toggle.setText("Iniciar servidor");
            port.setEditable(true);
            state.setText("● Desligado");
            state.setForeground(new Color(160, 60, 60));
            return;
        }
        try {
            int selected = Integer.parseInt(port.getText().trim());
            if (selected < 1 || selected > 65535) throw new IllegalArgumentException("Digite uma porta entre 1 e 65535.");
            listener = new TcpServer(accounts, this::note);
            listener.start(selected);
            port.setEditable(false);
            toggle.setText("Parar servidor");
            state.setText("● Online · " + selected);
            state.setForeground(new Color(21, 134, 86));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível iniciar o servidor:\n" + ex.getMessage(), "Verifique a porta", JOptionPane.WARNING_MESSAGE);
            note("ERRO: " + ex.getMessage());
        }
    }
    private void note(String message) {
        SwingUtilities.invokeLater(() -> {
            logs.append("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n\n");
            logs.setCaretPosition(logs.getDocument().getLength());
        });
        System.out.println(message);
    }
}
