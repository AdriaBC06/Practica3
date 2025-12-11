package lol.adriabonnin.netmonitor.server;

import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

// Envia notificacions UDP a tots els clients registrats
public class UdpBroadcaster {
    private final DatagramSocket udpSocket;
    private final ConcurrentMap<String, ClientInfo> clientsUdp;

    public UdpBroadcaster(int portServidor, ConcurrentMap<String, ClientInfo> clientsUdp) throws SocketException {
        this.udpSocket = new DatagramSocket(portServidor);
        this.clientsUdp = clientsUdp;
    }

    public void difondre(String missatge) {
        byte[] dades = missatge.getBytes();

        for (Map.Entry<String, ClientInfo> e : clientsUdp.entrySet()) {
            ClientInfo info = e.getValue();
            try {
                DatagramPacket paquet = new DatagramPacket(
                        dades, dades.length, info.getAdreca(), info.getPortUdp()
                );
                udpSocket.send(paquet);
            } catch (Exception ex) {
                System.err.println("Error enviant UDP: " + ex.getMessage());
            }
        }
    }

    public void tancar() {
        udpSocket.close();
    }
}
