import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SenderTCP {

    public static void main(String[] args) {
        List<DataOutputStream> outputStreamList = new ArrayList<>();
        List<Socket> clientSockets = new ArrayList<>();

        //Thread n1
        new Thread(() -> {
            while(true){
                try {
                    ServerSocket socket = new ServerSocket(50005);
                    System.out.println("Server started at port: 50005");
                    while(true) {
                        Socket connectionSocket = socket.accept();
                        System.out.println("Accepted connection from " + connectionSocket.getInetAddress());
                        outputStreamList.add(new DataOutputStream(new BufferedOutputStream(connectionSocket.getOutputStream())));
                        clientSockets.add(connectionSocket);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                    return;
            }
        }}).start();

        //Thread n2
        Thread n2 = new MusicSendingThread(outputStreamList);
        n2.start();

        //Thread n3, listen for commands
        Thread n3 = new ListenAtClientsThread(clientSockets);
        n3.start();
    }
}