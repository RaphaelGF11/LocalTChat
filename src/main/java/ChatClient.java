import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost"; // Remplacez par l'adresse IP du serveur
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connecté au serveur. Tapez vos messages ci-dessous.");

            // Thread pour lire les messages du serveur
            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println("Serveur: " + serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Lecture des messages de l'utilisateur et envoi au serveur
            String userMessage;
            while ((userMessage = consoleInput.readLine()) != null) {
                out.println(userMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
