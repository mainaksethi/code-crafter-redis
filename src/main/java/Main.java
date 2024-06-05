import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Completed Till Expiry

// TODO:
//  1. Handle Multiple Pings lessons failing
//      can't parse string with \n
// 2. Connection abruptly closing by server.

class ClientHandler implements Runnable { // Runnable enables threading
    private Socket clientSocket;
    private DataStore dataStore;
    public ClientHandler(Socket socket, DataStore dataStore) { this.clientSocket = socket;
    this.dataStore = dataStore;}

    @Override
    public void run() {
        System.out.println("Thread is getting called");
        try (PrintWriter printWriter =
                     new PrintWriter(this.clientSocket.getOutputStream())) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            boolean isHeyCommand = false;
            boolean isSetCommand = false;
            boolean isGetComand = false;
            while(bf.ready()) {
                String s = bf.readLine();
                if (s.equalsIgnoreCase("ping")) {
                    printWriter.write(HttpResponse.getPong);
                } else if (s.equalsIgnoreCase("hey")) {
                    isHeyCommand = true;
                } else if (s.equalsIgnoreCase("set")) {
                    isSetCommand = true;
                } else if (s.equalsIgnoreCase("get")) {
                    isGetComand = true;
                } else if(isHeyCommand && s.matches("[a-zA-Z]+")) {
                    printWriter.write(wrapAsOutput(s));
                } else if (isSetCommand && s.matches("[a-zA-Z]+")) {
                    String key = s;
                    String value = "";
                    SetCommandParams setCommandParams = parseSetCommand(bf);
                    dataStore.put(setCommandParams.key, setCommandParams.value, setCommandParams.expiry);
                } else if (isGetComand && s.matches("[a-zA-Z]+")) {
                    String key = s;
                    String value = dataStore.get(key);
                    printWriter.write(wrapAsOutput(value));
                }
            }
            printWriter.flush();
            System.out.println("Pong is sent.");
        } catch (IOException e) {
            System.out.println("There is an exception");
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SetCommandParams parseSetCommand(BufferedReader bf) throws IOException {
        boolean isExpiryPresent = false;
        Long expiry = null;
        String key = "";
        String value = "";
        boolean keyRead = false;
        while(bf.ready()) {
            String line = bf.readLine();
            if (isExpiryPresent && line.matches("[0-9]+")) {
                expiry = Long.parseLong(line);
            } else if (line.equalsIgnoreCase("px")) {
                isExpiryPresent = true;
            } else if (keyRead && line.matches("[a-zA-Z]+")) {
                value = line;
            } else if (line.matches("[a-zA-Z]+")) {
                key = line;
            }
        }
        return new SetCommandParams(key, value, expiry);
    }

    private String wrapAsOutput(String value) {
        return "+" + value + "\r\n";
    }
}

class SetCommandParams {
    String key = "";
    String value = "";
    Long expiry = null;

    public SetCommandParams(String key, String value, Long expiry) {
        this.key = key;
        this.value = value;
        this.expiry = expiry;
    }
}

class HttpResponse {
    public static String getPong = "+PONG\r\n";
}
class HttpServer {
    private int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() {
        DataStore dataStore = new DataStore();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // wait for new connections
                new Thread(new ClientHandler(clientSocket, dataStore))
                        .start(); // run on a new thread
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class Node {
    String value;
    Long createdAt;
    Long expiry;

    public Node(String value, Long createdAt, Long expiry) {
        this.value = value;
        this.createdAt = createdAt;
        this.expiry = expiry;
    }
}

class DataStore {
    private ConcurrentHashMap<String, Node> redisHashMap = new ConcurrentHashMap<>();

    public void put (String key, String value) {
        put(key, value, null);
    }

    public void put (String key, String value, Long expiryInSeconds) {
        redisHashMap.put(key, new Node(value, Instant.now().getEpochSecond(), null));
    }

    public String get(String key) {
        Node value = redisHashMap.get(key);
        if (value == null) {
            return "";
        }
        if (value.expiry != null && value.expiry + value.createdAt < Instant.now().getEpochSecond()) {
            return "";
        }
        return value.value;
    }

}

public class Main {
    public static void main(String[] args) {
        System.out.println("Program is starting");
        new HttpServer(6379).start();
    }
}


