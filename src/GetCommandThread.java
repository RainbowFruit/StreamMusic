import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class GetCommandThread extends Thread {

    private Socket socketToServer = null;

    GetCommandThread(Socket socket){
        socketToServer = socket;
    }

    @Override
    public void run() {
        super.run();
        Scanner in = new Scanner(System.in);
        byte[] packet = new byte[312 + 1];
        while(true){
            packet[312] = (byte)in.nextInt();
            if(socketToServer.isConnected()){
                try {
                    socketToServer.getOutputStream().write(packet, 0, 312 + 1);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
