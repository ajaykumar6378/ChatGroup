
import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    private static final Map<String, String> userDB = new HashMap<>(); // Store user IDs and names
    private static final List<Socket> activeClients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server is running...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected.");
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String userID;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                while (true) {
                    out.println("LOGIN_OR_CREATE"); // Send prompt to client
                    String command = in.readLine();

                    if ("LOGIN".equalsIgnoreCase(command)) {
                        if (handleLogin()) break;
                    } else if ("CREATE".equalsIgnoreCase(command)) {
                        if (handleAccountCreation()) break;
                    }
                }

                activeClients.add(clientSocket);
                out.println("LOGIN_SUCCESS");

                // Chat functionality
                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(userID + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    activeClients.remove(clientSocket);
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean handleLogin() throws IOException {
            out.println("ENTER_ID");
            String id = in.readLine();

            if (userDB.containsKey(id)) {
                this.userID = id;
                out.println("LOGIN_SUCCESS");
                System.out.println("User " + userDB.get(id) + " logged in.");
                return true;
            } else {
                out.println("LOGIN_FAILED");
                return false;
            }
        }

        private boolean handleAccountCreation() throws IOException {
            out.println("ENTER_ID");
            String id = in.readLine();

            if (userDB.containsKey(id)) {
                out.println("ID_EXISTS");
                return false;
            }

            out.println("ENTER_NAME");
            String name = in.readLine();

            userDB.put(id, name);
            this.userID = id;
            System.out.println("New user created: " + name);
            return true;
        }

        private void broadcastMessage(String message) {
            for (Socket client : activeClients) {
                try {
                    PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                    clientOut.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

