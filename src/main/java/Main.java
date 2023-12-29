import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


class ClientHandler implements Runnable { // Runnable enables threading
    private Socket clientSocket;
    public ClientHandler(Socket socket) { this.clientSocket = socket; }
    @Override
    public void run() {
        System.out.println("Thread is getting called");
        try (PrintWriter printWriter =
                     new PrintWriter(this.clientSocket.getOutputStream())) {
            printWriter.print(HttpResponse.getPong);
            printWriter.flush();
            System.out.println("Pong is sent.");
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
                System.out.println("aya");
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
