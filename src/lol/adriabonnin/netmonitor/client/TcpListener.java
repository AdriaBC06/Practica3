package lol.adriabonnin.netmonitor.client;

import java.io.BufferedReader;

/**
 * Fil opcional per escoltar respostes TCP asíncrones (si voleu mantenir enviaments TCP).
 */
public class TcpListener implements Runnable {
    private final BufferedReader in;
    private volatile boolean actiu = true;

    public TcpListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String linia;
            while (actiu && (linia = in.readLine()) != null) {
                System.out.println("\n[TCP] " + linia);
                System.out.print("> ");
            }

            // Si in.readLine() retorna null, la connexió TCP s'ha tancat (servidor apagat o conn. tancada)
            if (actiu) {
                System.out.println("\n[TCP] Connexió amb servidor tancada. El client es tancarà.");
                // Tanquem tot i sortim.
                System.exit(0);
            }
        } catch (Exception e) {
            if (actiu) System.err.println("Error al TcpListener: " + e.getMessage());
        }
    }

    public void aturar() {
        actiu = false;
        // No tanquem el BufferedReader aquí: el tanca el thread principal quan tanca el socket
    }
}
