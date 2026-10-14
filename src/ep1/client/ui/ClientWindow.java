package ep1.client.ui;

import ep1.client.RemoteApi;
import ep1.protocol.Json;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class ClientWindow extends JFrame {
    private static final Color NAVY = new Color(24, 44, 77);
    private static final Color BLUE = new Color(32, 86, 170);
    private static final Color SOFT = new Color(246, 248, 253);
    private final JTextField host = new JTextField("127.0.0.1", 15);
    private final JTextField port = new JTextField("34345", 7);
    private final JLabel sessionLabel = new JLabel("● Não autenticado");
    private final JLabel statusLabel = new JLabel("Aguardando operação");
    private final JTextArea traffic = new JTextArea();
    private final JTextField registerName = new JTextField(22);
    private final JTextField registerUsername = new JTextField(22);
    private final JPasswordField registerPassword = new JPasswordField(22);
    private final JTextField loginUsername = new JTextField(22);
    private final JPasswordField loginPassword = new JPasswordField(22);
    private final JTextField accountUsername = new JTextField(22);
    private final JTextField accountName = new JTextField(22);
    private final JPasswordField currentPassword = new JPasswordField(22);
    private final JPasswordField replacementPassword = new JPasswordField(22);
    private final JTextField sessionToken = new JTextField(34);
    private final JTextArea customJson = new JTextArea(7, 50);
    private final RemoteApi api = new RemoteApi(this::writeLog);
    private String token;
    private String signedUsername;

    public ClientWindow() {
        super("EP1 • Cliente de Cadastro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 820);
        setMinimumSize(new Dimension(790, 610));
        setLocationRelativeTo(null);
        getContentPane().setBackground(SOFT);
        setLayout(new BorderLayout(0, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(NAVY);
        top.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel title = new JLabel("CLIENTE  /  ÁREA DO USUÁRIO");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        JLabel subtitle = new JLabel("Solicitações JSON · conexão TCP individual · autenticação por token");
        subtitle.setForeground(new Color(194, 215, 242));
        top.add(title, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT, 13, 12));
        connection.setBackground(Color.WHITE);
        connection.setBorder(BorderFactory.createLineBorder(new Color(225, 232, 240)));
        JLabel addressLabel = new JLabel("IP / host:");
        addressLabel.setFont(addressLabel.getFont().deriveFont(Font.BOLD));
        connection.add(addressLabel);
        connection.add(host);
        connection.add(new JLabel("Porta:"));
        connection.add(port);
        sessionLabel.setForeground(new Color(143, 66, 66));
        connection.add(sessionLabel);
        body.add(connection, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tabs.addTab("1 · Cadastro", createRegistration());
        tabs.addTab("2 · Entrar e sair", createAccess());
        tabs.addTab("3 · Minha conta", createAccount());
        tabs.addTab("4 · JSON livre", createAdvanced());
        body.add(tabs, BorderLayout.CENTER);

        JPanel monitor = new JPanel(new BorderLayout(0, 8));
        monitor.setOpaque(false);
        JLabel label = new JLabel("Tráfego TCP — mensagens enviadas e recebidas");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        monitor.add(label, BorderLayout.NORTH);
        traffic.setEditable(false);
        traffic.setLineWrap(true);
        traffic.setWrapStyleWord(true);
        traffic.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        traffic.setBackground(new Color(15, 26, 45));
        traffic.setForeground(new Color(221, 235, 252));
        traffic.setMargin(new Insets(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(traffic);
        scroll.setPreferredSize(new Dimension(300, 230));
        monitor.add(scroll, BorderLayout.CENTER);
        JPanel resultLine = new JPanel(new BorderLayout(5, 0));
        resultLine.setOpaque(false);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        resultLine.add(statusLabel, BorderLayout.CENTER);
        JButton erase = new JButton("Limpar tráfego");
        erase.addActionListener(e -> traffic.setText(""));
        resultLine.add(erase, BorderLayout.EAST);
        monitor.add(resultLine, BorderLayout.SOUTH);
        body.add(monitor, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        writeLog("Cliente pronto. Informe o IP e a porta do servidor para se conectar.");
    }

    private JPanel createRegistration() {
        JPanel panel = panel("Crie um cadastro para autenticar no servidor.");
        layoutRow(panel, 1, "Nome completo", registerName);
        layoutRow(panel, 2, "Username", registerUsername);
        layoutRow(panel, 3, "Senha", registerPassword);
        addTip(panel, 4, "Senha: 8–20 caracteres, com maiúscula, minúscula, número e símbolo permitido.");
        JButton create = action("Cadastrar usuário");
        create.addActionListener(e -> {
            Map<String, Object> data = Json.object("name", registerName.getText().trim(), "username", registerUsername.getText().trim(), "password", new String(registerPassword.getPassword()));
            request("register", data, response -> {
                if (ok(response)) {
                    loginUsername.setText(registerUsername.getText().trim());
                    registerPassword.setText("");
                }
            });
        });
        addAction(panel, 5, create);
        return panel;
    }

    private JPanel createAccess() {
        JPanel panel = panel("Use suas credenciais para obter um token de autenticação.");
        layoutRow(panel, 1, "Username", loginUsername);
        layoutRow(panel, 2, "Senha", loginPassword);
        sessionToken.setEditable(false);
        sessionToken.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        layoutRow(panel, 3, "Token (UUID)", sessionToken);
        JButton enter = action("Entrar / login");
        enter.addActionListener(e -> request("login", Json.object("username", loginUsername.getText().trim(), "password", new String(loginPassword.getPassword())), response -> {
            if (ok(response)) {
                Object data = response.get("data");
                if (data instanceof Map<?, ?>) {
                    String received = Json.field(Json.asObject(data), "token");
                    if (received != null) {
                        token = received;
                        signedUsername = loginUsername.getText().trim();
                        accountUsername.setText(signedUsername);
                        sessionToken.setText(token);
                        sessionLabel.setText("● Autenticado: " + signedUsername);
                        sessionLabel.setForeground(new Color(20, 126, 77));
                        loginPassword.setText("");
                    }
                }
            }
        }));
        JButton leave = new JButton("Sair / logout");
        leave.addActionListener(e -> request("logout", Json.object("token", token), response -> {
            if (ok(response)) clearSession();
        }));
        addAction(panel, 4, enter, leave);
        addTip(panel, 5, "O token é guardado enquanto o cliente permanecer aberto. É invalidado ao sair.");
        return panel;
    }

    private JPanel createAccount() {
        JPanel panel = panel("Consulte, altere ou exclua apenas sua conta autenticada.");
        layoutRow(panel, 1, "Username", accountUsername);
        layoutRow(panel, 2, "Nome atualizado", accountName);
        JButton read = action("Consultar dados");
        read.addActionListener(e -> request("getuser", Json.object("token", token, "username", accountUsername.getText().trim()), response -> {
            if (ok(response) && response.get("data") instanceof Map<?, ?>) {
                Map<String, Object> data = Json.asObject(response.get("data"));
                accountName.setText(Json.field(data, "name"));
            }
        }));
        JButton changeName = action("Salvar nome");
        changeName.addActionListener(e -> request("updateusername", Json.object("token", token, "username", accountUsername.getText().trim(), "name", accountName.getText().trim()), null));
        addAction(panel, 3, read, changeName);
        layoutRow(panel, 4, "Senha antiga", currentPassword);
        layoutRow(panel, 5, "Nova senha", replacementPassword);
        JButton changePass = action("Alterar senha");
        changePass.addActionListener(e -> request("updateuserpassword", Json.object("token", token, "username", accountUsername.getText().trim(), "oldPassword", new String(currentPassword.getPassword()), "newPassword", new String(replacementPassword.getPassword())), response -> {
            if (ok(response)) { currentPassword.setText(""); replacementPassword.setText(""); }
        }));
        JButton remove = new JButton("Excluir conta");
        remove.setForeground(new Color(172, 47, 49));
        remove.addActionListener(e -> {
            int confirmation = JOptionPane.showConfirmDialog(this, "Excluir definitivamente o cadastro " + accountUsername.getText() + "?", "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmation == JOptionPane.YES_OPTION) request("deleteuser", Json.object("token", token, "username", accountUsername.getText().trim()), response -> {
                if (ok(response)) clearSession();
            });
        });
        addAction(panel, 6, changePass, remove);
        return panel;
    }

    private JPanel createAdvanced() {
        JPanel panel = panel("Teste manual: edite uma requisição JSON sem alterar o código-fonte.");
        customJson.setText("{\"method\":\"getuser\",\"data\":{\"token\":\"TOKEN_AQUI\",\"username\":\"usuario\"}}");
        customJson.setLineWrap(true);
        customJson.setWrapStyleWord(true);
        customJson.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane area = new JScrollPane(customJson);
        area.setPreferredSize(new Dimension(520, 160));
        GridBagConstraints c = constraints(1);
        c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.weightx = 1; c.weighty = 0.7;
        panel.add(area, c);
        JButton fill = new JButton("Inserir token atual");
        fill.addActionListener(e -> customJson.setText(customJson.getText().replace("TOKEN_AQUI", token == null ? "" : token)));
        JButton send = action("Enviar JSON");
        send.addActionListener(e -> {
            try {
                Json.asObject(Json.parse(customJson.getText()));
                sendRaw(customJson.getText(), null);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "JSON inválido: " + ex.getMessage(), "Corrija a mensagem", JOptionPane.ERROR_MESSAGE);
            }
        });
        addAction(panel, 2, fill, send);
        return panel;
    }

    private JPanel panel(String description) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225, 232, 240)), BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        JLabel intro = new JLabel(description);
        intro.setForeground(new Color(83, 100, 125));
        GridBagConstraints c = constraints(0);
        c.gridx = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        p.add(intro, c);
        GridBagConstraints filler = constraints(20);
        filler.gridx = 2; filler.weightx = 1; filler.weighty = 1;
        p.add(new JLabel(""), filler);
        return p;
    }
    private GridBagConstraints constraints(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 6, 8, 8);
        return c;
    }
    private void layoutRow(JPanel panel, int row, String name, javax.swing.JComponent component) {
        GridBagConstraints c = constraints(row);
        JLabel label = new JLabel(name);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        component.setPreferredSize(new Dimension(280, 30));
        panel.add(component, c);
    }
    private void addTip(JPanel panel, int row, String text) {
        GridBagConstraints c = constraints(row);
        c.gridwidth = 2;
        JLabel label = new JLabel(text);
        label.setForeground(new Color(83, 100, 125));
        panel.add(label, c);
    }
    private JButton action(String name) {
        JButton button = new JButton(name);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }
    private void addAction(JPanel panel, int row, JButton... buttons) {
        GridBagConstraints c = constraints(row);
        c.gridx = 0; c.gridwidth = 2;
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        group.setOpaque(false);
        for (JButton button : buttons) group.add(button);
        panel.add(group, c);
    }
    private void request(String method, Map<String, Object> data, Consumer<Map<String, Object>> onResult) {
        sendRaw(Json.stringify(Json.object("method", method, "data", data)), onResult);
    }
    private void sendRaw(String request, Consumer<Map<String, Object>> onResult) {
        String dest = host.getText().trim();
        int requestedPort;
        try { requestedPort = Integer.parseInt(port.getText().trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Porta deve ser um número válido."); return; }
        statusLabel.setText("Enviando requisição...");
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                return api.invokeJson(dest, requestedPort, request);
            }
            @Override protected void done() {
                try {
                    Map<String, Object> response = get();
                    Object code = response.get("statusCode");
                    statusLabel.setText("Status " + code + " — " + response.get("message"));
                    statusLabel.setForeground(ok(response) ? new Color(20, 126, 77) : new Color(172, 47, 49));
                    if (onResult != null) onResult.accept(response);
                } catch (Exception ex) {
                    String error = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                    writeLog("[FALHA NA CONEXÃO] " + error);
                    statusLabel.setText("Falha de conexão: " + error);
                    statusLabel.setForeground(new Color(172, 47, 49));
                }
            }
        }.execute();
    }
    private boolean ok(Map<String, Object> reply) {
        Object status = reply.get("statusCode");
        return status instanceof Number && ((Number) status).intValue() >= 200 && ((Number) status).intValue() < 300;
    }
    private void clearSession() {
        token = null;
        signedUsername = null;
        sessionToken.setText("");
        sessionLabel.setText("● Não autenticado");
        sessionLabel.setForeground(new Color(143, 66, 66));
    }
    private void writeLog(String message) {
        SwingUtilities.invokeLater(() -> {
            traffic.append("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n\n");
            traffic.setCaretPosition(traffic.getDocument().getLength());
        });
        System.out.println(message);
    }
}
