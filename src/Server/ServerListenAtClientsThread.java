import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ServerListenAtClientsThread extends Thread {

    private List<Socket> clientSockets = null;
    int sizeOfMusicData = 312;
    private byte[] buffer = new byte[sizeOfMusicData + 1];

    ServerListenAtClientsThread(List<Socket> list){
        this.clientSockets = list;
    }

    @Override
    public void run() {

        super.run();
        while(true){
            for (Socket socket : clientSockets) {
                try {
                    if(socket.getInputStream().read(buffer) > 0){
                        System.out.println("Received command number: " + buffer[sizeOfMusicData]);
                        //TODO: Handle commands
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
