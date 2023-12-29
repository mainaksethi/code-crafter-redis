import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClientExample {

    public static void main(String[] args) throws UnknownHostException,
            IOException, ClassNotFoundException, InterruptedException,
            UnknownHostException {
        //get the localhost IP address, if server is running on some other IP, you need to use that
        InetAddress host = InetAddress.getLocalHost();
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        for(int i=0; i<5;i++) {
            //establish socket connection to server
            socket = new Socket(host.getHostName(), 6379);
            //write to socket using ObjectOutputStream
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            Thread.sleep(100);

            BufferedReader stdIn = new BufferedReader(
                    new InputStreamReader(System.in));
            String userInput;

            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                System.out.println("echo: " + in.readLine());
            }
            out.close();
            in.close();
            stdIn.close();
            socket.close();
        }
    }
}
