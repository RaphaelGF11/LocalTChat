import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showInitialOptions);
    }

    private static void showInitialOptions() {
        JFrame frame = new JFrame("Choisir une option");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));

        JButton serverButton = new JButton("Ouvrir un serveur de discussion");
        JButton clientButton = new JButton("Rejoindre un serveur de discussion");

        serverButton.addActionListener(e -> {
            frame.dispose();
            startServer();
            String username = askUsername();
            try {
                String localIp = InetAddress.getLocalHost().getHostAddress();
                new ChatClientGUI(localIp, username);
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
        });

        clientButton.addActionListener(e -> {
            frame.dispose();
            String serverAddress = askServerAddress();
            if (isServerReachable(serverAddress, 34196)) {
                String username = askUsername();
                new ChatClientGUI(serverAddress, username);
            } else {
                JOptionPane.showMessageDialog(null, "Le serveur n'est pas accessible. Veuillez vérifier l'adresse et réessayer.", "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                showInitialOptions();
            }
        });

        panel.add(new JLabel("Que voulez-vous faire ?"));
        panel.add(serverButton);
        panel.add(clientButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void startServer() {
        Thread serverThread = new Thread(() -> ChatServer.main(null));
        serverThread.start();
    }

    private static String askServerAddress() {
        return JOptionPane.showInputDialog(null, "Entrez l'adresse du serveur:", "Adresse du serveur", JOptionPane.QUESTION_MESSAGE);
    }

    private static String askUsername() {
        return JOptionPane.showInputDialog(null, "Entrez votre pseudo:", "Pseudo", JOptionPane.QUESTION_MESSAGE);
    }

    private static boolean isServerReachable(String serverAddress, int port) {
        try (Socket socket = new Socket(serverAddress, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
