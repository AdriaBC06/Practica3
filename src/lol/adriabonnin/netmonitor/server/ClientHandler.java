package lol.adriabonnin.netmonitor.server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manejador per cada client TCP. Rep comandes i interactua amb TcpServer.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TcpServer servidor;
    private final String idClient;

    private BufferedReader entrada;
    private PrintWriter sortida;
    private volatile boolean actiu = true;

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

            // Benvinguda i instruccions
            sortida.println("Benvingut a NetMonitor. Envia 'UDPPORT <port>' per rebre notificacions UDP.");

            String linia;
            while (actiu && (linia = entrada.readLine()) != null) {
                linia = linia.trim();

                if (linia.equalsIgnoreCase("TIME")) {
                    String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    sortida.println(hora);

                } else if (linia.toUpperCase().startsWith("ECHO ")) {
                    // Extract text i difondre
                    String text = linia.substring(5);
                    servidor.enviarEchoAAltres(idClient, text);
                    sortida.println("Enviat als altres clients.");

                } else if (linia.equalsIgnoreCase("COUNT")) {
                    sortida.println("Clients: " + servidor.getClientsConnectats());

                } else if (linia.toUpperCase().startsWith("UDPPORT")) {
                    String[] parts = linia.split("\\s+");
                    if (parts.length < 2) {
                        sortida.println("Ús: UDPPORT <port>");
                        continue;
                    }
                    try {
                        int port = Integer.parseInt(parts[1]);
                        if (port <= 0 || port > 65535) {
                            sortida.println("Port UDP invàlid.");
                            continue;
                        }
                        servidor.registrarUdp(idClient, new ClientInfo(socket.getInetAddress(), port));
                        sortida.println("Port UDP registrat.");
                    } catch (NumberFormatException nfe) {
                        sortida.println("Port UDP invàlid (no és un número).");
                    }

                } else if (linia.equalsIgnoreCase("BYE")) {
                    sortida.println("Adeu!");
                    tancar();

                } else if (linia.equalsIgnoreCase("SHUTDOWN")) {
                    // Permetre apagar només si el client és local
                    if (socket.getInetAddress().isLoopbackAddress() || socket.getInetAddress().getHostAddress().equals("127.0.0.1")) {
                        sortida.println("Servidor apagant-se...");
                        servidor.apagar();
                        tancar();
                    } else {
                        sortida.println("Comanda SHUTDOWN només des de localhost per motius de seguretat.");
                    }

                } else {
                    sortida.println("Comanda invàlida.");
                }
            }
        } catch (Exception e) {
            System.err.println("ClientHandler (" + idClient + ") error: " + e.getMessage());
        } finally {
            tancar();
        }
    }

    // Envia una línia per TCP a aquest client
    public void enviar(String msg) {
        if (sortida != null) sortida.println(msg);
    }

    // Tancar connexió d'aquest handler i informar el servidor
    public void tancar() {
        if (!actiu) return; // evita doble tancament
        actiu = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}

        servidor.desregistrarClient(idClient);
    }
}
