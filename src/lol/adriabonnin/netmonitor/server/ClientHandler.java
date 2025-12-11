package lol.adriabonnin.netmonitor.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable{
    private Socket socket;
    private static int activeClients = 0;
    public ClientHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        activeClients++;
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true); // amb autoflush

            String missatge = br.readLine();
            System.out.println("Missatge client: " + missatge);

            switch (missatge.toUpperCase()){
                case "TIME":
                    LocalDateTime data = LocalDateTime.now();
                    DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
                    pw.println(data.format(formatObj));
                    break;
                case "COUNT":
                    System.out.println(activeClients);
                case "BYE":
                    activeClients--;
                    socket.close();
                default:
                    pw.println("El que has introduit no és una opció: \n" +
                            "TIME -> returna l'hora actual. \n" +
                            "ECHO <text> -> mostra el texte a la resta de client \n" +
                            "COUNT -> retorna el número de clients connectats \n" +
                            "BYE -> desconecta el client \n" +
                            "SHUTDOWN -> apaga el servidor");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
