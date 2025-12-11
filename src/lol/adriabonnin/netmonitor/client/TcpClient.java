package lol.adriabonnin.netmonitor.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClient {

    public static final String HOST = "localhost";
    public static final int PORT_TCP = 5000;

    public static void main(String[] args) {

        DatagramSocket socketUdp = null;
        Socket socketTcp = null;
        UdpNotificationListener listener = null;

        try {
            // Crear port UDP propi
            socketUdp = new DatagramSocket(0);
            int portLocalUdp = socketUdp.getLocalPort();

            // Connexió TCP
            socketTcp = new Socket(HOST, PORT_TCP);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socketTcp.getInputStream()));
            PrintWriter sortida = new PrintWriter(socketTcp.getOutputStream(), true);

            // Iniciar fil de notificacions
            listener = new UdpNotificationListener(socketUdp);
            new Thread(listener).start();

            // Registrar UDPPORT al servidor
            sortida.println("UDPPORT " + portLocalUdp);
            System.out.println("Servidor: " + entrada.readLine());

            // Menú
            Scanner sc = new Scanner(System.in);
            String comanda;

            System.out.println("Comandes disponibles: TIME, ECHO <txt>, COUNT, BYE, SHUTDOWN");

            while (true) {
                System.out.print("> ");
                comanda = sc.nextLine();

                sortida.println(comanda);
                String resposta = entrada.readLine();

                if (resposta != null)
                    System.out.println("Servidor: " + resposta);

                if (comanda.equalsIgnoreCase("BYE") || comanda.equalsIgnoreCase("SHUTDOWN"))
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error client: " + e.getMessage());
        } finally {
            if (listener != null) listener.aturar();
            try { if (socketTcp != null) socketTcp.close(); } catch (Exception ignored) {}
            System.out.println("Client tancat.");
        }
    }
}
