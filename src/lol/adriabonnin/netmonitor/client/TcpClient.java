package lol.adriabonnin.netmonitor.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client que obre connexió TCP amb el servidor i també un socket UDP per rebre notificacions.
 * - envia "UDPPORT <portLocal>" per registrar-se
 * - pot enviar comandes: TIME, ECHO <text>, COUNT, BYE, SHUTDOWN
 */
public class TcpClient {

    public static final String HOST = "localhost";
    public static final int PORT_TCP = 5000;

    public static void main(String[] args) {

        DatagramSocket socketUdp = null;
        Socket socketTcp = null;
        UdpNotificationListener udpListener = null;
        TcpListener tcpListener = null;
        Thread tcpThread = null;

        try {
            // Crear port UDP propi
            socketUdp = new DatagramSocket(0);
            int portLocalUdp = socketUdp.getLocalPort();

            // Connexió TCP
            socketTcp = new Socket(HOST, PORT_TCP);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socketTcp.getInputStream()));
            PrintWriter sortida = new PrintWriter(socketTcp.getOutputStream(), true);

            // Iniciar TcpListener (opcional: si voleu veure respostes TCP asíncrones)
            tcpListener = new TcpListener(entrada);
            tcpThread = new Thread(tcpListener);
            tcpThread.start();

            // Iniciar fil de notificacions UDP
            udpListener = new UdpNotificationListener(socketUdp);
            new Thread(udpListener).start();

            // Registrar UDPPORT al servidor
            sortida.println("UDPPORT " + portLocalUdp);

            // Menú
            Scanner sc = new Scanner(System.in);
            String comanda;

            System.out.println("Comandes disponibles: TIME, ECHO <txt>, COUNT, BYE, SHUTDOWN");

            while (true) {
                System.out.print("> ");
                comanda = sc.nextLine();
                if (comanda == null || comanda.trim().isEmpty()) continue;

                sortida.println(comanda);

                // No llegim resposta directament: TcpListener s'encarrega d'imprimir respostes i ECHO TCP.
                if (comanda.equalsIgnoreCase("BYE") || comanda.equalsIgnoreCase("SHUTDOWN")) break;
            }

        } catch (Exception e) {
            System.err.println("Error client: " + e.getMessage());
        } finally {
            if (tcpListener != null) tcpListener.aturar();
            try { if (tcpThread != null) tcpThread.join(1000); } catch (InterruptedException ignored) {}
            if (udpListener != null) udpListener.aturar();
            try { if (socketTcp != null) socketTcp.close(); } catch (Exception ignored) {}
            if (socketUdp != null && !socketUdp.isClosed()) socketUdp.close();
            System.out.println("Client tancat.");
        }
    }
}
