package lol.adriabonnin.netmonitor.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Escolta notificacions UDP en paral·lel i les mostra per consola.
 */
public class UdpNotificationListener implements Runnable {

    private final DatagramSocket socketUdp;
    private volatile boolean actiu = true;

    public UdpNotificationListener(DatagramSocket socketUdp) {
        this.socketUdp = socketUdp;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (actiu) {
            try {
                DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                socketUdp.receive(paquet);

                String msg = new String(paquet.getData(), 0, paquet.getLength());

                // Si el servidor envia la notificació de shutdown, sortim
                if (msg.startsWith("SERVER_SHUTDOWN")) {
                    System.out.println("\n[UDP] " + msg.replaceFirst("SERVER_SHUTDOWN\\|?", ""));
                    System.out.println("Servidor apagat — el client es tancarà.");
                    // Assegura't d'aturar i tancar el socket UDP i sortir de l'aplicació
                    try { socketUdp.close(); } catch (Exception ignored) {}
                    System.exit(0);
                }

                // Parsejar missatges ECHO per fer-los més llegibles
                if (msg.startsWith("ECHO|")) {
                    String[] parts = msg.split("\\|", 3);
                    if (parts.length == 3) {
                        System.out.println("\n[ECHO de " + parts[1] + "] " + parts[2]);
                    } else {
                        System.out.println("\n[UDP] " + msg);
                    }
                } else {
                    System.out.println("\n[UDP] " + msg);
                }

                System.out.print("> ");

            } catch (Exception e) {
                if (socketUdp.isClosed()) break;
            }
        }
    }

    public void aturar() {
        actiu = false;
        socketUdp.close();
    }
}
