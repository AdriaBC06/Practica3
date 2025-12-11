package lol.adriabonnin.netmonitor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class TcpServer {

    public static final int PORT_TCP = 5000;
    public static final int PORT_UDP = 6000;

    // Llistes globals de clients TCP i UDP
    private final ConcurrentMap<String, ClientHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientInfo> clientsUdp = new ConcurrentHashMap<>();

    private UdpBroadcaster broadcaster;
    private ServerSocket servidorTcp;
    private boolean funcionant = true;

    public static void main(String[] args) throws IOException {
        new TcpServer().iniciar();
    }

    public void iniciar() throws IOException {
        servidorTcp = new ServerSocket(PORT_TCP);
        broadcaster = new UdpBroadcaster(PORT_UDP, clientsUdp);

        System.out.println("Servidor escoltant al port TCP " + PORT_TCP);

        ExecutorService pool = Executors.newCachedThreadPool();

        while (funcionant) {
            Socket socket = servidorTcp.accept();
            String id = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

            ClientHandler manejador = new ClientHandler(socket, this, id);
            handlers.put(id, manejador);

            pool.submit(manejador);

            broadcaster.difondre("NEW_CLIENT|" + id + " s'ha connectat");
            broadcaster.difondre("TOTAL|Actualment hi ha " + handlers.size() + " clients connectats");
        }
    }

    public void registrarUdp(String id, ClientInfo info) {
        clientsUdp.put(id, info);
    }

    public void desregistrarClient(String id) {
        handlers.remove(id);
        clientsUdp.remove(id);

        broadcaster.difondre("CLIENT_LEFT|" + id + " s'ha desconnectat");
        broadcaster.difondre("TOTAL|Actualment hi ha " + handlers.size() + " clients connectats");
    }

    public void enviarEchoAAltres(String idOrigen, String text) {
        for (var e : handlers.entrySet()) {
            if (!e.getKey().equals(idOrigen)) {
                e.getValue().enviar("ECHO de " + idOrigen + ": " + text);
            }
        }
    }

    public void apagar() {
        funcionant = false;
        try { servidorTcp.close(); } catch (Exception ignored) {}
        broadcaster.tancar();
    }

    public int getClientsConnectats() {
        return handlers.size();
    }
}
