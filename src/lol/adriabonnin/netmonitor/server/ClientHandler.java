package lol.adriabonnin.netmonitor.server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TcpServer servidor;
    private final String idClient;

    private BufferedReader entrada;
    private PrintWriter sortida;
    private boolean actiu = true;

    public ClientHandler(Socket socket, TcpServer servidor, String idClient) {
        this.socket = socket;
        this.servidor = servidor;
        this.idClient = idClient;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sortida = new PrintWriter(socket.getOutputStream(), true);

            String linia;
            while (actiu && (linia = entrada.readLine()) != null) {

                if (linia.equalsIgnoreCase("TIME")) {
                    String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    sortida.println(hora);

                } else if (linia.startsWith("ECHO ")) {
                    servidor.enviarEchoAAltres(idClient, linia.substring(5));
                    sortida.println("Enviat als altres clients.");

                } else if (linia.equalsIgnoreCase("COUNT")) {
                    sortida.println("Clients: " + servidor.getClientsConnectats());

                } else if (linia.startsWith("UDPPORT")) {
                    int port = Integer.parseInt(linia.split(" ")[1]);
                    servidor.registrarUdp(idClient, new ClientInfo(socket.getInetAddress(), port));
                    sortida.println("Port UDP registrat.");

                } else if (linia.equalsIgnoreCase("BYE")) {
                    sortida.println("Adeu!");
                    tancar();

                } else if (linia.equalsIgnoreCase("SHUTDOWN")) {
                    sortida.println("Servidor apagant-se...");
                    servidor.apagar();
                    tancar();

                } else {
                    sortida.println("Comanda invàlida.");
                }
            }
        } catch (Exception ignored) {
        } finally {
            tancar();
        }
    }

    public void enviar(String msg) {
        if (sortida != null) sortida.println(msg);
    }

    private void tancar() {
        if (!actiu) return;
        actiu = false;
        try { socket.close(); } catch (Exception ignored) {}
        servidor.desregistrarClient(idClient);
    }
}
