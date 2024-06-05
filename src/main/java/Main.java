import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


class ClientHandler implements Runnable { // Runnable enables threading
    private Socket clientSocket;
    public ClientHandler(Socket socket) { this.clientSocket = socket; }
    @Override
    public void run() {
        System.out.println("Thread is getting called");
        try (PrintWriter printWriter =
                     new PrintWriter(this.clientSocket.getOutputStream())) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            while(bf.ready()) {
                String s = bf.readLine();
                if (s.equalsIgnoreCase("ping")) {
                    printWriter.write(HttpResponse.getPong);
                }
            }
            printWriter.flush();
            System.out.println("Pong is sent.");
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("There is an exception");
            System.out.println("IOException: " + e.getMessage());
        }
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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // wait for new connections
                new Thread(new ClientHandler(clientSocket))
                        .start(); // run on a new thread
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("Program is starting");
        new HttpServer(6379).start();
    }
}
