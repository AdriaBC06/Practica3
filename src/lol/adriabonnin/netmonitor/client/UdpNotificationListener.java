package lol.adriabonnin.netmonitor.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

// Escolta notificacions UDP en paral·lel
public class UdpNotificationListener implements Runnable {

    private final DatagramSocket socketUdp;
    private boolean actiu = true;

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
                System.out.println("\n[UDP] " + msg);
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
