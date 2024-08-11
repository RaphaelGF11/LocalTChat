import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatClientGUI extends JFrame {
    private PrintWriter out;
    private Socket socket;
    private BufferedReader in;
    private JTextArea chatArea;
    private JTextField inputField;
    private List<Parameter> parameters;
    private String username;

    public ChatClientGUI(String serverAddress, String username) {
        this.username = username;
        parameters = new ArrayList<>();
        parameters.add(new Parameter("serverHost", "String", serverAddress, "Adresse du serveur"));
        parameters.add(new Parameter("serverPort", "Int", 34196, "Port du serveur")); // Port par défaut 34196
        parameters.add(new Parameter("useSSL", "Boolean", false, "Utiliser SSL"));

        setTitle("Local Chat (" + serverAddress + ")"); // Mettre à jour le titre avec l'IP du serveur
        setSize(400, 300);
        setMinimumSize(new Dimension(200, 160)); // Taille minimale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        JButton sendButton = new JButton("↑");
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(settingsButton.getFont().deriveFont(22f)); // Augmenter la taille du texte
        sendButton.setFont(sendButton.getFont().deriveFont(22f)); // Augmenter la taille du texte

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(settingsButton, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(chatScrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        settingsButton.addActionListener(e -> {
            new SettingsWindow(this, parameters);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
                System.exit(0);
            }
        });

        setVisible(true);

        new Thread(this::startClient).start();
    }

    private void startClient() {
        try {
            Thread.sleep(1000); // Attendre un peu pour s'assurer que le serveur est prêt
            String serverHost = (String) parameters.get(0).getValue();
            int serverPort = (int) parameters.get(1).getValue();
            socket = new Socket(serverHost, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send the username to the server
            if (username != null && !username.trim().isEmpty()) {
                out.println(username);
            }

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                final String message = serverMessage;
                SwingUtilities.invokeLater(() -> {
                    if (message.contains("Vous avez été expulsé du serveur.")) {
                        JOptionPane.showMessageDialog(null, "Vous avez été expulsé du serveur.", "Expulsé", JOptionPane.WARNING_MESSAGE);
                        closeConnection();
                        System.exit(0);
                    } else {
                        chatArea.append(message + "\n");
                    }
                });
            }
        } catch (IOException | InterruptedException e) {
            if (!(e instanceof java.net.SocketException && "Socket closed".equals(e.getMessage()))) {
                e.printStackTrace();
            }
        } finally {
            closeConnection();
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            if (message.startsWith("/")) {
                handleCommand(message);
            } else {
                out.println(username + ": " + message);
            }
            inputField.setText("");
        }
    }

    private void handleCommand(String command) {
        out.println(command);
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
