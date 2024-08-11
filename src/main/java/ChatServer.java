import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class ChatServer {
    private static final int PORT = 34196;
    private static boolean running = true;
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static final List<String> chatHistory = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> opList = new ConcurrentHashMap<>();
    private static final Map<String, String> bannedUsers = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private static final Map<String, Integer> commandPermissions = new HashMap<>();
    private static String creator;

    public static void main(String[] args) {
        loadOpList();
        initializeCommandPermissions();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            String localIp = getLocalIpAddress();
            logger.info("Serveur démarré, adresse : " + localIp);
            broadcastMessage("Serveur démarré, adresse : " + localIp, "serveur");

            new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
                    String consoleCommand;
                    while ((consoleCommand = consoleReader.readLine()) != null) {
                        handleCommand(consoleCommand, "serveur", 3, true);
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Erreur de lecture de la console", e);
                }
            }).start();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.execute(new ClientHandler(clientSocket));
                } catch (SocketException e) {
                    logger.info("Serveur arrêté.");
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur du serveur", e);
        }
    }

    private static void initializeCommandPermissions() {
        commandPermissions.put("stop", 3);
        commandPermissions.put("say", 1);
        commandPermissions.put("reset", 4);
        commandPermissions.put("op", 3);
        commandPermissions.put("deop", 3);
        commandPermissions.put("kick", 2);
        commandPermissions.put("ban", 3);
        commandPermissions.put("unban", 3);
        commandPermissions.put("deban", 3);
        commandPermissions.put("clear", 2);
        commandPermissions.put("showip", 3);
        commandPermissions.put("list", 0);
        commandPermissions.put("help", 0);
    }

    public static void handleCommand(String command, String user, int userLevel, boolean isConsole) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].substring(1);
        String arg = parts.length > 1 ? parts[1] : "";

        int commandLevel = commandPermissions.getOrDefault(cmd, -1);
        if (commandLevel == -1) {
            sendMessage(user, "Commande inconnue: " + cmd);
            return;
        }

        if (userLevel >= commandLevel || (userLevel == 3 && commandLevel == 4)) {
            switch (cmd) {
                case "stop":
                    stop();
                    break;
                case "say":
                    broadcastMessage(arg, user);
                    break;
                case "reset":
                    resetServer();
                    break;
                case "op":
                    if (!arg.isEmpty()) {
                        opUser(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /op");
                    }
                    break;
                case "deop":
                    if (!arg.isEmpty()) {
                        deopUser(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /deop");
                    }
                    break;
                case "kick":
                    if (!arg.isEmpty()) {
                        kickUser(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /kick");
                    }
                    break;
                case "ban":
                    if (!arg.isEmpty()) {
                        banUser(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /ban");
                    }
                    break;
                case "unban":
                case "deban":
                    if (!arg.isEmpty()) {
                        unbanUser(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /unban");
                    }
                    break;
                case "clear":
                    clearMessages(arg);
                    break;
                case "showip":
                    if (!arg.isEmpty()) {
                        showIp(arg);
                    } else {
                        sendMessage(user, "Argument manquant pour la commande /showip");
                    }
                    break;
                case "list":
                    listUsers(user);
                    break;
                case "help":
                    showHelp(user, arg);
                    break;
                default:
                    sendMessage(user, "Commande inconnue: " + cmd);
            }
        } else {
            sendMessage(user, "Permission refusée.");
        }
    }

    public static void stop() {
        running = false;
        try {
            new Socket("localhost", PORT).close(); // Fermer la connexion pour quitter le bloc accept
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadOpList() {
        try (BufferedReader reader = new BufferedReader(new FileReader("ops.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    opList.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException e) {
            logger.warning("Impossible de charger le fichier ops.txt");
        }
    }

    private static void saveOpList() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("ops.txt"))) {
            for (Map.Entry<String, Integer> entry : opList.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            logger.warning("Impossible de sauvegarder le fichier ops.txt");
        }
    }

    private static void broadcastMessage(String message, String sender) {
        synchronized (clients) {
            chatHistory.add(sender + ": " + message);
            if (chatHistory.size() > 100) {
                chatHistory.remove(0);
            }
            for (PrintWriter client : clients.values()) {
                client.println(sender + ": " + message);
            }
        }
    }

    private static void sendMessage(String user, String message) {
        PrintWriter out = clients.get(user);
        if (out != null) {
            out.println(message);
        } else if ("serveur".equals(user)) {
            System.out.println(message);
        }
    }

    private static String getLocalIpAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        }
        return "127.0.0.1"; // Fallback
    }

    private static void resetServer() {
        broadcastMessage("Le serveur est réinitialisé...", "serveur");
        chatHistory.clear();
        opList.clear();
        saveOpList();
        bannedUsers.clear();
        broadcastMessage("Serveur réinitialisé.", "serveur");
    }

    private static void opUser(String user) {
        opList.put(user, 2);
        saveOpList();
        broadcastMessage(user + " a été promu opérateur.", "serveur");
    }

    private static void deopUser(String user) {
        opList.remove(user);
        saveOpList();
        broadcastMessage(user + " a été rétrogradé.", "serveur");
    }

    private static void kickUser(String user) {
        if (clients.containsKey(user)) {
            PrintWriter userOut = clients.get(user);
            userOut.println("Vous avez été expulsé du serveur.");
            clients.remove(user);
            broadcastMessage(user + " a été expulsé.", "serveur");
        } else {
            sendMessage(user, "Utilisateur introuvable: " + user);
        }
    }

    private static void banUser(String user) {
        if (clients.containsKey(user)) {
            bannedUsers.put(user, clients.get(user).toString());
            PrintWriter userOut = clients.get(user);
            userOut.println("Vous avez été banni du serveur.");
            clients.remove(user);
            broadcastMessage(user + " a été banni.", "serveur");
        } else {
            sendMessage(user, "Utilisateur introuvable: " + user);
        }
    }

    private static void unbanUser(String user) {
        if (bannedUsers.containsKey(user)) {
            bannedUsers.remove(user);
            broadcastMessage(user + " a été débanni.", "serveur");
        } else {
            sendMessage(user, "Utilisateur introuvable: " + user);
        }
    }

    private static void clearMessages(String arg) {
        int numMessages = arg.isEmpty() ? chatHistory.size() : Integer.parseInt(arg);
        for (int i = 0; i < numMessages; i++) {
            if (!chatHistory.isEmpty()) {
                chatHistory.remove(chatHistory.size() - 1);
            }
        }
        broadcastMessage("Les messages ont été effacés.", "serveur");
    }

    private static void showIp(String user) {
        if (clients.containsKey(user)) {
            sendMessage(user, "Adresse IP de " + user + ": " + clients.get(user).toString());
        } else {
            sendMessage(user, "Utilisateur introuvable: " + user);
        }
    }

    private static void listUsers(String requester) {
        StringBuilder userList = new StringBuilder("Utilisateurs connectés: ");
        for (String client : clients.keySet()) {
            userList.append(client).append(", ");
        }
        sendMessage(requester, userList.toString());
    }

    private static void showHelp(String user, String command) {
        if (command.isEmpty()) {
            sendMessage(user, "Commandes disponibles: /stop, /say, /reset, /op, /deop, /kick, /ban, /unban, /clear, /showip, /list, /help");
        } else {
            switch (command) {
                case "stop":
                    sendMessage(user, "/stop - Arrête le serveur");
                    break;
                case "say":
                    sendMessage(user, "/say <text> - Envoie le texte comme un simple message");
                    break;
                case "reset":
                    sendMessage(user, "/reset - Réinitialise le serveur");
                    break;
                case "op":
                    sendMessage(user, "/op <user> - Ajoute un utilisateur connecté à la liste des opérateurs");
                    break;
                case "deop":
                    sendMessage(user, "/deop <user> - Retire un opérateur de la liste des opérateurs");
                    break;
                case "kick":
                    sendMessage(user, "/kick <user> - Déconnecte un utilisateur connecté au serveur");
                    break;
                case "ban":
                    sendMessage(user, "/ban <user> - Bannit l'adresse IP et le pseudo d'un utilisateur");
                    break;
                case "unban":
                case "deban":
                    sendMessage(user, "/unban <user> - Débannit l'adresse IP et le pseudo d'un utilisateur");
                    break;
                case "clear":
                    sendMessage(user, "/clear <int> - Efface des messages (sans option : tous les messages, avec 'int' efface les 'int' derniers messages)");
                    break;
                case "showip":
                    sendMessage(user, "/showip <user> - Affiche l'adresse IP d'un utilisateur connecté");
                    break;
                case "list":
                    sendMessage(user, "/list - Affiche la liste des utilisateurs connectés");
                    break;
                case "help":
                    sendMessage(user, "/help <command> - Affiche la liste des commandes ou explique l'utilisation d'une commande spécifique");
                    break;
                default:
                    sendMessage(user, "Commande inconnue: " + command);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private String username;
        private InetAddress clientAddress;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                this.out = out;
                this.clientAddress = clientSocket.getInetAddress();

                // Read the username
                username = in.readLine();
                if (username != null && !username.trim().isEmpty()) {
                    if (creator == null) {
                        creator = username;
                        opList.put(username, 3); // Le créateur a des permissions de niveau 3
                    }
                    clients.put(username, out);
                    broadcastMessage(username + " s'est connecté", "serveur");
                }

                // Send chat history to the new client
                synchronized (chatHistory) {
                    for (String message : chatHistory) {
                        out.println(message);
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    logger.info("Message reçu: " + message);
                    if (message.startsWith("/")) {
                        handleCommand(message, username, getPermissionLevel(username), false);
                    } else {
                        broadcastMessage(message, username);
                    }
                }
            } catch (SocketException e) {
                logger.info("Client déconnecté.");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erreur avec le client", e);
            } finally {
                if (username != null && !username.trim().isEmpty()) {
                    broadcastMessage(username + " s'est déconnecté", "serveur");
                    clients.remove(username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Erreur lors de la fermeture de la socket client", e);
                }
            }
        }

        private int getPermissionLevel(String user) {
            if (user.equals("serveur") || (user.equals(creator) && clientAddress.getHostAddress().equals("127.0.0.1"))) {
                return 3;
            }
            return opList.getOrDefault(user, 1);
        }
    }
}
