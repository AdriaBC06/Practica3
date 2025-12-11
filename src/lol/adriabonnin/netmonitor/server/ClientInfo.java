package lol.adriabonnin.netmonitor.server;

import java.net.InetAddress;

/**
 * Guarda la informació bàsica d'un client per enviar-li UDP.
 */
public class ClientInfo {
    private final InetAddress adreca;
    private final int portUdp;

    public ClientInfo(InetAddress adreca, int portUdp) {
        this.adreca = adreca;
        this.portUdp = portUdp;
    }

    public InetAddress getAdreca() { return adreca; }
    public int getPortUdp() { return portUdp; }
}
