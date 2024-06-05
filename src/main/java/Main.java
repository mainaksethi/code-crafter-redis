import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


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
                    clientSocket.getOutputStream().write(HttpResponse.getPong.getBytes());
                } else if (s.equalsIgnoreCase("hey")) {
                    isHeyCommand = true;
                } else if (s.equalsIgnoreCase("set")) {
                    isSetCommand = true;
                } else if (s.equalsIgnoreCase("get")) {
                    isGetComand = true;
                } else if(isHeyCommand && s.matches("[a-zA-Z]+")) {
                    clientSocket.getOutputStream().write(wrapAsOutput(s).getBytes());
                } else if (isSetCommand && s.matches("[a-zA-Z]+")) {
                    String key = s;
                    String value = "";
                    while(bf.ready()) {
                        String line = bf.readLine();
                        if (line.matches("[a-zA-Z]+")) {
                            value = line;
                        }
                    }
                    dataStore.put(key, value);
                } else if (isGetComand && s.matches("[a-zA-Z]+")) {
                    String key = s;
                    String value = dataStore.get(key);
                    clientSocket.getOutputStream().write(wrapAsOutput(value).getBytes());
                }
            }
            System.out.println("Pong is sent.");
        } catch (IOException e) {
            System.out.println("There is an exception");
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private String wrapAsOutput(String value) {
        return "+" + value + "\r\n";
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

class DataStore {
    private ConcurrentHashMap<String, String> redisHashMap = new ConcurrentHashMap<>();

    public void put (String key, String value) {
        redisHashMap.put(key, value);
    }

    public String get(String key) {
        return redisHashMap.getOrDefault(key, "");
    }

}

public class Main {
    public static void main(String[] args) {
        System.out.println("Program is starting");
        new HttpServer(6379).start();
    }
}


