package lol.adriabonnin.netmonitor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {
    static void main() throws IOException {
        ServerSocket server = new ServerSocket(5000);
        System.out.println("Escoltant al port 5000");

        while(true){
            Socket socket = server.accept();
            System.out.println("Client conectat");
            new Thread(new ClientHandler(socket)).start();
        }
    }
}
