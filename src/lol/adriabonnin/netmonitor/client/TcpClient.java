package lol.adriabonnin.netmonitor.client;

import java.io.*;
import java.net.*;

public class TcpClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5000);

        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter bw = new PrintWriter(socket.getOutputStream(), true);

        bw.println("TIME");

        String respuesta = br.readLine();
        System.out.println("Servidor dice: " + respuesta);

        socket.close();
    }
}
