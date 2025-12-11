package lol.adriabonnin.netmonitor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Servidor TCP principal. Manté llistes concurrents de handlers TCP i clients UDP.
 * Envia notificacions UDP mitjançant UdpBroadcaster.
 */
public class TcpServer {

    public static final int PORT_TCP = 5000;
    public static final int PORT_UDP = 6000;

    // Llistes globals de clients TCP i UDP (clau: idClient = ip:portTcp)
    private final ConcurrentMap<String, ClientHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientInfo> clientsUdp = new ConcurrentHashMap<>();

    private UdpBroadcaster broadcaster;
    private ServerSocket servidorTcp;
    private volatile boolean funcionant = true;

    public static void main(String[] args) throws IOException {
        new TcpServer().iniciar();
    }

    public void iniciar() throws IOException {
        servidorTcp = new ServerSocket(PORT_TCP);
        broadcaster = new UdpBroadcaster(PORT_UDP, clientsUdp);

        System.out.println("Servidor escoltant al port TCP " + PORT_TCP);

        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            while (funcionant) {
                try {
                    Socket socket = servidorTcp.accept();
                    String id = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

                    ClientHandler manejador = new ClientHandler(socket, this, id);
                    handlers.put(id, manejador);
                    pool.submit(manejador);

                    // No difondre NEW_CLIENT aquí: s'enviarà quan el client envii 'UDPPORT <port>'.
                    System.out.println("Connexió acceptada: " + id);
                } catch (java.net.SocketException se) {
                    if (!funcionant) {
                        System.out.println("Accept aturat per apagada del servidor.");
                        break;
                    } else {
                        throw se;
                    }
                }
            }
        } finally {
            // Tancament net
            try {
                if (servidorTcp != null && !servidorTcp.isClosed()) servidorTcp.close();
            } catch (Exception ignored) {}

            // Tancar tots els handlers
            for (var h : handlers.values()) {
                try { h.tancar(); } catch (Exception ignored) {}
            }
            handlers.clear();

            // tancar broadcaster
            if (broadcaster != null) broadcaster.tancar();

            pool.shutdownNow();
            System.out.println("Servidor finalitzat.");
        }
    }

    /**
     * Registrar/un client amb la seva informacio UDP. Envia notificacions NEW_CLIENT i TOTAL
     * només quan és un registre nou.
     */
    public void registrarUdp(String id, ClientInfo info) {
        // comprova si ja hi ha un (ip,port) igual per a evitar col·lisions reals
        boolean portEnUs = clientsUdp.values().stream()
                .anyMatch(ci -> ci.getAdreca().equals(info.getAdreca()) && ci.getPortUdp() == info.getPortUdp());

        if (portEnUs) {
            System.out.println("El port UDP " + info.getPortUdp() + " ja està en ús per aquesta IP.");
            // No sobreescrivim la inscripció original: informem i sortim
            return;
        }

        boolean nou = (clientsUdp.put(id, info) == null);
        if (nou) {
            try {
                broadcaster.difondre("NEW_CLIENT|" + id + " s'ha connectat");
                broadcaster.difondre("TOTAL|Actualment hi ha " + handlers.size() + " clients connectats");
            } catch (Exception ex) {
                System.err.println("Error difonent NEW_CLIENT: " + ex.getMessage());
            }
        } else {
            System.out.println("Actualitzat UDP de " + id + " -> " + info.getAdreca() + ":" + info.getPortUdp());
        }
    }

    /**
     * Desregistra un client completament (TCP i UDP) i envia notificacions.
     */
    public void desregistrarClient(String id) {
        handlers.remove(id);
        clientsUdp.remove(id);

        // Notificacions (si hi ha clients registrats es difondran)
        try {
            broadcaster.difondre("CLIENT_LEFT|" + id + " s'ha desconnectat");
            broadcaster.difondre("TOTAL|Actualment hi ha " + handlers.size() + " clients connectats");
        } catch (Exception ex) {
            System.err.println("Error difonent CLIENT_LEFT: " + ex.getMessage());
        }
    }

    /**
     * Enviar ECHO: enviem per TCP als altres clients i també fem broadcast per UDP (format ECHO|id|text).
     * Si preferiu només UDP, podeu eliminar l'enviament TCP.
     */
    public void enviarEchoAAltres(String idOrigen, String text) {
        // 1) Envia per TCP als altres clients (opcional)
        for (var e : handlers.entrySet()) {
            if (!e.getKey().equals(idOrigen)) {
                e.getValue().enviar("ECHO de " + idOrigen + ": " + text);
            }
        }

        // 2) També difondre l'ECHO via UDP a tots els clients registrats
        if (broadcaster != null) {
            String missatge = "ECHO|" + idOrigen + "|" + text;
            broadcaster.difondre(missatge);
        }
    }

    /**
     * Apaga el servidor de manera neta: notifica per UDP, tanca handlers i ServerSocket.
     */
    public void apagar() {
        funcionant = false;
        System.out.println("Apagant servidor...");

        // 1) Notificar per UDP a tots els clients registrats que el servidor s'apaga
        if (broadcaster != null) {
            try {
                broadcaster.difondre("SERVER_SHUTDOWN|El servidor s'està apagant");
            } catch (Exception ex) {
                System.err.println("Error enviant SERVER_SHUTDOWN via UDP: " + ex.getMessage());
            }
        }

        // 2) Tancar tots els handlers (això tancarà els sockets TCP de cada client,
        //    provocant que als clients el readLine() retorni null i es puguin tancar)
        for (var entry : handlers.entrySet()) {
            try {
                entry.getValue().tancar(); // tancar() ja fa desregistrarClient()
            } catch (Exception ignored) {}
        }
        handlers.clear();
        clientsUdp.clear();

        // 3) Tanquem el ServerSocket per forçar que accept() deixi de bloquejar-se
        try {
            if (servidorTcp != null && !servidorTcp.isClosed()) servidorTcp.close();
        } catch (IOException e) {
            System.err.println("Error tancant servidorTcp: " + e.getMessage());
        }

        // 4) Finalment tanquem el broadcaster
        if (broadcaster != null) {
            try { broadcaster.tancar(); } catch (Exception ignored) {}
            broadcaster = null;
        }

        System.out.println("Servidor apagat completament.");
    }

    public int getClientsConnectats() {
        return handlers.size();
    }
}
